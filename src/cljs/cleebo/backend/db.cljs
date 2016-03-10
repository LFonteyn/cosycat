(ns cleebo.backend.db
  (:require-macros [cleebo.env :refer [cljs-env]]))

(def default-db
  "defines app default state"
  {:active-panel :query-panel
   :notifications {}
   :ls-modal false
   :throbbing? {:main-panel false}
   :settings {:delay 7500}
   :session {:query-opts {:corpus (first (cljs-env :blacklab :corpora))
                          :context 5
                          :size 10}
             :query-results {:query-size 0
                             :query-str ""
                             :status {:status :ok :status-content ""}
                             :from 0
                             :to 0}
             :results-by-id {}
             :results []}})



