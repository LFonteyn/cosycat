(ns cleebo.core
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [cleebo.components.http-server :refer [new-http-server]]
            [cleebo.components.db :refer [new-db]]
            [cleebo.components.cqp :refer [new-cqi-client]]
            [cleebo.components.blacklab :refer [new-bl]]
            [cleebo.components.figwheel :refer [new-figwheel]]
            [cleebo.components.ws :refer [new-ws]]
            [clojure.pprint :as pprint]
            [taoensso.timbre :as timbre]
            [environ.core :refer [env]]
            [clojure.string :as str]))

(def config-map
  {:port (:port env)
   :database-url (:database-url env)
   :cqp-init-file (get-in env [:cqp :cqp-init-file])
   :blacklab-paths-map (get-in env [:blacklab :blacklab-paths-map])})

(defn create-system [config-map]
  (let [{:keys [handler port cqp-init-file database-url blacklab-paths-map]} config-map]
    (-> (component/system-map
         :cqi-client (new-cqi-client {:init-file cqp-init-file})
         :blacklab (new-bl blacklab-paths-map)
         :db (new-db {:url database-url})
         :ws (new-ws)
         :figwheel (new-figwheel)
         :http-server (new-http-server {:port port
                                        :components [:cqi-client :db :ws :blacklab]}))
        (component/system-using
         {:http-server [:cqi-client :db :ws :blacklab]
          :blacklab    [:ws]
          :ws          [:db]}))))

(defonce system nil)

(defn init []
  (println "\n\nStarting server with enviroment:")
  (pprint/pprint (select-keys env [:host :database-url :cqp :blacklab]))
  (println "\n")  
  (alter-var-root #'system (constantly (create-system config-map))))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system component/stop))

(defn run []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'cleebo.core/run))

(defn -main [& args]
  (let [system (create-system config-map)]
    (.addShutdownHook 
     (Runtime/getRuntime) 
     (Thread. (fn []
                (.stop system))))
    (.start system)))
