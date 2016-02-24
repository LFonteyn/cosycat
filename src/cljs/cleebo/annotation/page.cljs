(ns cleebo.annotation.page
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [react-bootstrap.components :as bs]
            [cleebo.utils :refer [parse-time]]
            [cleebo.components :refer [error-panel]]))

(defn hit-token [{:keys [id word match marked]}]
  (fn [{:keys [id word match marked anns]}]
    (let [info (if match "info" "")]
      [:td.highlighted {:class (str info) :data-id id} word])))

(defn hit-row [{:keys [hit id meta]}]
  (fn [{:keys [hit id meta]}]
    (into [:tr]
          (for [token hit]
            ^{:key (str id "-" (:id token))} [hit-token token]))))

(def example-anns [{:key "animacy" :value "M"} {:key "aspect" :value "perfect"}])

(defn new-annotation-button []
  [bs/button
   {:bsClass "info"
    :on-click #(.log js/console "NEW")}
   [bs/glyphicon {:glyph "plus"}]])

(defn annotation-row [{:keys [hit meta id]}]
  (let [style {:padding-top "5px" :padding-bottom "5px"}]
    (fn [{:keys [hit meta id]}]
      (into
       [:tr]
       (for [{:keys [word anns] :as token} hit]
         ^{:key (str (:id token))}
         [:td 
          [:table; {:style {:font-size "13px" :max-width "100%" :min-height "50px"}}
           [:thead]
           [:tbody
            (if anns
              (for [{:keys [ann username timestamp]} (seq anns)
                    :let [[k] (keys ann)
                          [v] (vals ann)]]
                ^{:key (str (:id token) "-anns-" k)}
                [:tr {:style {:padding "15px" :font-size "12px"}}
                 [:td {:style (merge style {:text-align "left"})} (str k)]
                 [:td {:style (merge style {:text-align "right"})}
                  [bs/label [:span {:class "hint--bottom" :data-hint username} v]]]])
              [:tr])]]])))))

(defn annotation-queue [marked-hits]
  (fn [marked-hits]
    [bs/table
     {:responsive true
      :bordered true
      :id "tableAnnotation"}
     [:thead]
     [:tbody {:style {:font-size "13px"}}
      (doall
       (interleave
        (for [[_ {:keys [id] :as hit-map}] @marked-hits]
          ^{:key id} [hit-row hit-map])
        (for [[_ {:keys [id] :as hit-map}] @marked-hits]
          ^{:key (str id "anns")} [annotation-row hit-map])))]]))

(defn back-to-query-button []
  [bs/button {:href "#/query"}
   [:span {:style {:padding-right "10px"}}
    [:i.zmdi.zmdi-city-alt]]
   "Back to query"])

(defn annotation-panel []
  (let [marked-hits (re-frame/subscribe [:marked-hits])]
    (fn []
      [:div.container
       {:style {:width "100%" :padding "0 10px 0 10px"}}
       (if (zero? (count @marked-hits))
         [error-panel
          :status "No hits marked for annotation..."
          :status-content [back-to-query-button]]
         [annotation-queue marked-hits])])))
