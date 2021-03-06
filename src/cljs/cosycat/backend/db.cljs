(ns cosycat.backend.db)

(defn default-opts-map [key]
  (case key
    :sort-opts {:position "match" :attribute "word" :facet "insensitive"}
    :filter-opts {:attribute "title" :value "random"}))

(def verbosity-settings
  {:signup true
   :login true
   :logout true
   :remove-project true
   :new-project-issue true
   :update-project-issue true
   :close-project-issue true
   :add-project-user true
   :new-project-user true
   :remove-project-user true
   :new-project-user-role true
   :new-query-metadata true
   :update-query-metadata true
   :drop-query-metadata true
   :new-user-avatar true
   :new-user-info true})

(defn default-settings
  [& {:keys [corpora] :or {corpora []}}]
  (let [corpus (first (map :corpus corpora))]
    {:notifications {:delay 7500
                     :verbosity verbosity-settings}
     :query {:query-opts {:context 5 :from 0 :page-size 5}
             :sort-opts []
             :filter-opts []
             :snippet-opts {:snippet-size 50 :snippet-delta 25}
             :corpus corpus}}))

(defn get-project-settings [db project-name]
  (or (get-in db [:projects project-name :settings]) ;project-related settings
      (get-in db [:me :settings])                    ;global settings
      (default-settings :corpora (:corpora db))))

(def default-session
  {:active-panel :front-panel
   :active-project nil})

(defn default-project-session [project]
  {:query {:results {:results-summary {}
                     :results []
                     :results-by-id {}}}
   :review {:results {:results-summary {}
                      :results-by-id {}}
            :query-opts {:context 5
                         :window 5
                         :size 5
                         :sort-opts []
                         :query-map {:corpus #{}
                                     :ann {:key {:string ""
                                                 :as-regex? false}
                                           :value {:string ""
                                                   :as-regex? false}}
                                     :username #{}
                                     :timestamp {}
                                     :hit-id {:string ""
                                              :as-regex? false}}}}
   :snippet {}
   :status {}
   :components {:panel-open {:query-frame true :annotation-frame false}
                :active-project-frame :users
                :issue-filters {:status "open" :type "all"}
                :event-filters {:type "all"}
                :open-hits #{}
                :review-input-open? {:key false :value false}
                :toggle-hits "none"
                :token-field :word}
   :filtered-users (into #{} (map :username (:users project)))})

(def default-history {:app-events []})

(def default-project-history {:query []})
