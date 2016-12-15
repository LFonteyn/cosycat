(ns cosycat.review.review-toolbar
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [react-bootstrap.components :as bs]
            [react-date-range.core :refer [calendar date-range]]
            [cosycat.components :refer [dropdown-select]]
            [cosycat.utils :refer [->map]]
            [cosycat.app-utils :refer [dekeyword disjconj]]
            [taoensso.timbre :as timbre]))

;;; Ann & context
(defn on-input-open [label state-atom open?]
  (fn []
    (let [path [:review-input-open? label]]
      (if-not @open?
        (re-frame/dispatch [:set-project-session-component path true])
        (do (re-frame/dispatch [:set-project-session-component path false])
            (reset! state-atom nil))))))

(defn text-input [{:keys [label state-atom placeholder]}]
  (let [open? (re-frame/subscribe [:project-session :components :review-input-open? label])]
    (fn [{:keys [label state-atom placeholder]}]
      [:div.form-group
       {:style {:padding "0 5px"}}
       [:div.input-group
        [:input.form-control
         {:type "text"
          :style {:width "90px"}
          :disabled (not @open?)
          :placeholder (or placeholder (dekeyword label))
          :value @state-atom
          :on-change #(reset! state-atom (.-value (.-target %)))}]
        [:div.input-group-addon
         {:onClick (on-input-open label state-atom open?)
          :style {:cursor "pointer"}}
         [bs/glyphicon
          {:glyph "pencil"}]]]])))

(defn select-fn [path]
  (fn [v]
    (re-frame/dispatch [:set-project-session (into [:review :query-opts] path) v])))

(defn main-inputs [{:keys [key-state-atom value-state-atom]}]
  (let [context (re-frame/subscribe [:project-session :review :query-opts :context])]
    (fn [{:keys [key-state-atom value-state-atom]}]
      [:form.form-inline
       [text-input {:label :key :state-atom key-state-atom :placeholder "Ann Key"}]
       [text-input {:label :value :state-atom value-state-atom :placeholder "Ann Value"}]
       [dropdown-select
        {:label "context: "
         :header "Select a token context size"
         :options (map #(->map % %) (range 20))
         :model @context
         :select-fn (select-fn [:context])}]])))

;;; Username & Corpora
(defn multiple-select-row [{:keys [key label selected?]} on-select]
  (fn [{:keys [key label selected?]} on-select]
    [:li
     [:div.checkbox
      [:label
       [:input
        {:type "checkbox"
         :on-change #(on-select key)
         :checked @selected?}]
       label]]]))

(defn multiple-select-button [{:keys [label on-select on-clear title options has-selection?]}]
  (let [show? (reagent/atom false), target (reagent/atom nil)]
    (fn [{:keys [label on-select on-clear title options has-selection?]}]
      [bs/button-group
       [bs/button
        {:onClick #(do (swap! show? not) (reset! target (.-target %)))
         :bsStyle (if has-selection? "primary" "default")}
        label]
       [bs/button
        {:onClick #(on-clear)}
        [bs/glyphicon {:glyph "erase"}]]
       [bs/overlay
        {:show @show?
         :target (fn [] @target)
         :placement "bottom"
         :rootClose true
         :onHide #(swap! show? not)}
        [bs/popover
         {:id "popover"
          :title (reagent/as-component
                  [:div.container-fluid.pad
                   [:div.row.pad
                    [:div.col-sm-10.pull-left [:h5 title]]
                    [:div.col-sm-2.text-right
                     {:style {:font-size "12px" :text-align "right"}}
                     [:span
                      {:style {:cursor "pointer" :line-height "3.3"}
                       :onClick #(swap! show? not)}
                      "✕"]]]])}
         [:ul
          {:style {:list-style "none"}}
          (doall
           (for [{:keys [key] :as opts} options]
             ^{:key key} [multiple-select-row opts on-select]))]]]])))

(defn on-select-multiple [label]
  (let [path (conj [:review :query-opts :query-map] label)]
    (fn [key]
      (re-frame/dispatch [:update-project-session path (fn [the-set] (disjconj the-set key))]))))

(defn on-clear-multiple [label]
  (fn []
    (re-frame/dispatch [:set-project-session [:review :query-opts :query-map label] #{}])))

(defn get-selected [label value]
  (re-frame/subscribe [:review-query-opts-selected? label value]))

(defn select-range [data]
  (let [{:keys [startDate endDate] :as cljs-data} (js->clj data :keywordize-keys true)
        new-timestamp {:from (.toDate startDate) :to (.toDate endDate)}]
    (re-frame/dispatch
     [:set-project-session [:review :query-opts :query-map :timestamp] new-timestamp])))

(defn time-input-button []
  (let [open? (reagent/atom false), target (reagent/atom nil)
        model (re-frame/subscribe [:project-session :review :query-opts :query-map :timestamp])
        today (js/Date.)]
    (fn []
      [bs/button-group
       [bs/button
        {:onClick #(do (swap! open? not) (reset! target (.-target %)))
         :bsStyle (if (empty? @model) "default" "primary")}
        "Time range"]
       [bs/button
        {:onClick #(re-frame/dispatch
                    [:set-project-session [:review :query-opts :query-map :timestamp] {}])}
        [bs/glyphicon {:glyph "erase"}]]
       [bs/overlay
        {:show @open?
         :target (fn [] @target)
         :placement "bottom"
         :rootClose true
         :onHide #(swap! open? not)}
        [bs/popover
         {:id "popover"
          :style {:max-width "none"}}
         [date-range
          (let [{:keys [from to]} @model
                start (js/moment. from)
                end (js/moment. to)]
            (cond-> {:onInit identity
                     :onChange select-range
                     :maxDate (js/moment. (js/Date.))}
              (not (empty? @model)) (assoc :startDate start :endDate end)))]]]])))

(defn rest-inputs []
  (let [users (re-frame/subscribe [:users])
        corpora (re-frame/subscribe [:corpora :corpus])
        user-select (re-frame/subscribe [:project-session :review :query-opts :query-map :username])
        corpus-select (re-frame/subscribe [:project-session :review :query-opts :query-map :corpus])]
    (fn []
      [bs/button-toolbar
       {:class "pull-right"}
       [time-input-button]
       [multiple-select-button
        {:label "Corpora"
         :on-select (on-select-multiple :corpus)
         :on-clear (on-clear-multiple :corpus)
         :title "Select annotation corpus"
         :options (for [corpus @corpora
                        :let [selected? (get-selected :corpus corpus)]]
                    {:key corpus :label corpus :selected? selected?})
         :has-selection? (not (empty? @corpus-select))}]
       [multiple-select-button
        {:label "Users"
         :on-select (on-select-multiple :username)
         :on-clear (on-clear-multiple :username)
         :title "Select annotation authors"
         :options (for [username (map :username @users)
                        :let [selected? (get-selected :username username)]]
                    {:key username :label username :selected? selected?})
         :has-selection? (not (empty? @user-select))}]])))

(defn submit [{:keys [key-state-atom value-state-atom]}]
  (fn [{:keys [key-state-atom value-state-atom]}]
    [bs/button
     {:bsStyle "primary"}
     "Submit"]))

(defn review-toolbar []
  (let [key-state-atom (reagent/atom nil), value-state-atom (reagent/atom nil)]
    (fn []
      [:div.row
       [:div.col-lg-5.col-md-6.text-left
        [main-inputs {:key-state-atom key-state-atom :value-state-atom value-state-atom}]]
       [:div.col-lg-6.col-md-5 [rest-inputs]]
       [:div.col-lg-1.col-md-1.pull-right
        [submit {:key-state-atom key-state-atom :value-state-atom value-state-atom}]]])))
