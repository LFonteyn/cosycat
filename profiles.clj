{:dev {:dependencies [[com.cemerick/piggieback "0.2.1"]
                      [figwheel-sidecar "0.5.2"]]
       :source-paths ["env/dev/clj"]
       :cljsbuild {:builds {:app {:source-paths ["env/dev/cljs"]}}}
       :env {:dev? true
             :host "localhost"
             :database-url "mongodb://127.0.0.1:27017/cleeboDev"
             :port 3000
             :session-expires 900       ;in minutes
             :corpora ["mbg-small"]
             :blacklab-paths-map
             {"mbg-small" "/home/enrique/cleebo-dep/indices/blacklab/mbg-index-small/"}}}
 :test {:env {:database-url "mongodb://127.0.0.1:27017/cleeboTest"
              :corpora ["mbg-small"]
              :blacklab-paths-map
              {"mbg-small" "/home/enrique/cleebo-dep/indices/blacklab/mbg-index-small/"}}}
 :uberjar {:source-paths ["env/prod/clj"]
           :hooks [leiningen.cljsbuild]
           :prep-tasks ["compile" ["cljsbuild" "once"]]
           :env {:prod? true
                 :database-url "mongodb://127.0.0.1:27017/cleebo"
                 :port 3000
                 :session-expires 90    ;in minutes
                 :corpora ["brown"]
                 :blacklab-paths-map {}}
           :omit-source true
           :aot :all
           :cljsbuild {:jar true
                       :builds {:app {:source-paths ["env/prod/cljs"]
                                      :compiler {:optimizations :advanced
                                                 :closure-defines {goog.DEBUG true}
                                                 :pretty-print false}}}}}}
