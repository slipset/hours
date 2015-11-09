(ns hours.handler
    (:require
      [clojure.java.jdbc :as sql]
      [ring.middleware.file :refer [wrap-file]]
      [ring.adapter.jetty :refer [run-jetty]]
      [ring.util.response :refer [response redirect header]]      
      [compojure.core :refer :all]
      [compojure.route :as route]
      [compojure.handler :as handler]
      [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
      [ring.middleware.content-type :refer [wrap-content-type]]
      [ring.middleware.not-modified :refer [wrap-not-modified]]      
      [ring.util.response :refer [file-response resource-response
                                  status content-type]]      
      [cemerick.friend        :as friend]
      [clj-time.core :as t]
      [clj-time.format :as f]
      [clj-time.coerce :as c]      
      [hours.time :as time]
      [hours.layout :as layout]
      [hours.period :as period]      
      [hours.client :as client]
      [hours.prjct :as prjct]
      [hours.reports.routes :as report]
      [hours.registration.routes :as registration]
      [hours.projects.routes :as projects]      
      [hours.migrations :as migrations]
      [hours.security :as security]
      [environ.core :refer [env]])
    (:gen-class)) 

(def db-spec {:connection {:connection-uri (env :jdbc-database-url)}})

(defn add-client! [user-id name]
  (client/add-client<! {:name name :user_id user-id} db-spec)
  (ring.util.response/redirect "/client/"))


(defn edit-period! [user-id id date start end description project-id]
  (period/edit-period! db-spec user-id id date start end description project-id)
  (ring.util.response/redirect "/user/register/start"))

(defn delete-period! [user-id id]
  (period/delete-period! db-spec user-id id)
  (ring.util.response/redirect "/user/register/start"))

(defn show-projects [user-id client-id]
  (layout/display-projects (prjct/user-client-projects {:user_id user-id :client_id client-id} db-spec)))

(defn text-html [resp]
  (-> (response resp)
      (header "Content-type" "text/html; charset=utf-8")))

(defroutes client-routes
  (GET "/" [user-id] (text-html (layout/display-clients (client/user-clients {:user_id user-id} db-spec))))
  (GET "/:client-id/projects" [client-id user-id] (text-html (show-projects user-id client-id)))
  (GET "/add" [] (text-html (layout/display-add-client)))
  (POST "/add" [name user-id] (add-client! user-id name)))


(defroutes period-routes
  (GET "/:id" [id user-id] (text-html (layout/display-edit-period (first (period/by-id {:user_id user-id :id id} db-spec))
                                                                  (prjct/user-projects {:user_id user-id } db-spec))))
  (POST "/:id" [user-id id date start end project-id description]  (edit-period! user-id id date start end description project-id))
  (GET "/:id/delete" [id user-id] (delete-period! user-id id)))

(defroutes app-routes
  (GET "/" [user-id] (if (empty? user-id) (text-html (layout/render-login)) (redirect "/user")))
  (GET "/status" request (layout/display-status-page request))
  (context "/user" request (friend/wrap-authorize registration/user-routes #{security/user}))
  (context "/client" request (friend/wrap-authorize client-routes #{security/user}))
  (context "/project" request (friend/wrap-authorize projects/project-routes #{security/user}))
  (context "/period" request (friend/wrap-authorize period-routes #{security/user}))
  (context "/report" request (friend/wrap-authorize report/report-routes #{security/user}))      
  (friend/logout (ANY "/logout" request (ring.util.response/redirect "/")))
  (rfn request (-> {:body  (layout/display-not-found request)} (status 404) text-html)))


(defn wrap-add-user-id [handler uid-param]
  (fn [request]
    (handler (assoc-in request [:params uid-param] (security/user-id request)))))

(defn wrap-add-db [handler db]
  (fn [request]
    (handler (assoc-in request [:params :db] db))))

(defn wrap-page-template [handler]
  (fn [request]
    (update-in (handler request) [:body] (layout/get-html5 (security/user-info request)))))

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
