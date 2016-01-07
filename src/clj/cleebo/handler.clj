(ns cleebo.handler
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre]
            [compojure.core
             :refer [GET POST ANY defroutes wrap-routes]]
            [compojure.route :refer [not-found resources]]
            [prone.middleware :refer [wrap-exceptions]]
            [cleebo.views.error :refer [error-page]]
            [cleebo.views.cleebo :refer [cleebo-page]]
            [cleebo.views.landing :refer [landing-page]]
            [cleebo.views.about :refer [about-page]]
            [cleebo.views.login :refer [login-page]]
            [ring.util.response :refer [redirect response]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.anti-forgery
             :refer [wrap-anti-forgery *anti-forgery-token*]]
            [ring.middleware.transit
             :refer [wrap-transit-response wrap-transit-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.nested-params :refer [wrap-nested-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring-ttl-session.core :refer [ttl-memory-store]]
            [buddy.auth :refer [authenticated?]]
            [buddy.auth.middleware
             :refer [wrap-authentication wrap-authorization]]
            [cleebo.routes.auth
             :refer [safe auth-backend login-authenticate on-login-failure signup]]
            [cleebo.routes.ws :refer [ws-handler-http-kit]]))

(defn is-logged? [req]
  (get-in req [:session :identity]))

(def about-route
  (safe
   (fn [req] (about-page :logged? (is-logged? req)))
   {:login-uri "/login" :is-ok? authenticated?}))

(def cleebo-route
  (safe
   (fn [req] (cleebo-page :csrf *anti-forgery-token*))
   {:login-uri "/login" :is-ok? authenticated?}))

(defroutes app-routes
  (GET "/" req (landing-page :logged? (is-logged? req)))
  (GET "/login" req (login-page :csrf *anti-forgery-token*))
  (POST "/login" req (login-authenticate on-login-failure))
  (POST "/signup" req (signup req))
  (GET "/about" req (about-route req))
  (GET "/cleebo" req (cleebo-route req))
  (ANY "/logout" req (-> (redirect "/") (assoc :session {})))
  (GET "/ws" req (ws-handler-http-kit req))
  (resources "/")
  (not-found (error-page :status 404 :title "Page not found!!")))

;;; middleware
(defn wrap-internal-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (error-page
         {:status 500
          :title "Something very bad happened!"
          :message (str t)})))))

(defn wrap-debug [handler]
  (fn [req]
    (timbre/info req)
    (handler req)))

(defn wrap-base [handler]
  (-> handler   
      wrap-debug
      wrap-reload
      (wrap-authorization auth-backend)
      (wrap-authentication auth-backend)
      (wrap-anti-forgery {:read-token (fn [req] (get-in req [:params :csrf]))})
      (wrap-session {:store (ttl-memory-store (* 30 60))})
      (wrap-transit-params {:encoding :json-verbose})
      wrap-keyword-params
      wrap-params
      (wrap-transit-response {:encoding :json-verbose})
      wrap-exceptions
      wrap-internal-error))

(def web-app (wrap-routes #'app-routes wrap-base))

(defn wrap-app-component [handler components]
  (fn [req]
    (handler (assoc req :components components))))

(defn make-handler [component]
  (let [components (select-keys component (:components component))]
    (-> app-routes
        (wrap-app-component components)
        (wrap-routes wrap-base))))

