(ns hours.handler
    (:require
      [ring.middleware.file :refer [wrap-file]]
      [ring.adapter.jetty :refer [run-jetty]]
      [ring.util.response :refer [response redirect header content-type]]
      [compojure.core :refer :all]
      [compojure.route :as route]
      [compojure.handler :as handler]
      [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
      [ring.middleware.content-type :refer [wrap-content-type]]
      [ring.middleware.not-modified :refer [wrap-not-modified]]
      [ring.util.response :refer [file-response resource-response
                                  status content-type]]
      [cemerick.friend        :as friend]
      [hours.layout :as layout]
      [hours.reports.routes :as report]
      [hours.registration.routes :as registration]
      [hours.projects.routes :as projects]
      [hours.clients.routes :as clients]
      [hours.periods.routes :as periods]
      [hours.migrations :as migrations]
      [hours.security :as security]
      [environ.core :refer [env]])
    (:gen-class)) 

(def db-spec {:connection {:connection-uri (env :jdbc-database-url)}})

(defroutes app-routes
  (GET "/" [user-id] (if (empty? user-id) (layout/render-login) (redirect "/user")))
  (GET "/status" request (layout/display-status-page request))
  (context "/user" request (friend/wrap-authorize registration/user-routes #{security/user}))
  (context "/client" request (friend/wrap-authorize clients/client-routes #{security/user}))
  (context "/project" request (friend/wrap-authorize projects/project-routes #{security/user}))
  (context "/period" request (friend/wrap-authorize periods/period-routes #{security/user}))
  (context "/report" request (friend/wrap-authorize report/report-routes #{security/user}))      
  (friend/logout (ANY "/logout" request (ring.util.response/redirect "/")))
  (rfn request (-> {:body  (layout/display-not-found request)} (status 404))))

(defn wrap-add-user-id [handler uid-param]
  (fn [request]
    (handler (assoc-in request [:params uid-param] (security/user-id request)))))

(defn wrap-add-db [handler db]
  (fn [request]
    (handler (assoc-in request [:params :db] db))))

(defn wrap-page-template [handler]
  (fn [request]
    (let [response (update-in (handler request) [:body] (layout/get-html5 (security/user-info request)))]
      (if (.startsWith (:body response) "<!DOCTYPE html>")
        (content-type response  "text/html; charset=utf-8")
        response))))

(def app
  (-> #'app-routes
        (wrap-page-template)
        (wrap-add-user-id :user-id)
        (wrap-add-db db-spec)
        (friend/authenticate (security/friend-config db-spec))
        (wrap-file "resources/public")
        (wrap-content-type)
        (wrap-not-modified)
        handler/site))

(defn start-jetty  [port]
  (run-jetty #'app {:port port
                         :join? false}))

(defn -main []
  (migrations/migrate)
  (let [port (Integer. (or (env :port) "3000"))]
    (start-jetty port)))
