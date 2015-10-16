(ns hours.handler
    (:require
      [clojure.java.jdbc :as sql]
      [ring.middleware.file :refer [wrap-file]]
      [ring.adapter.jetty :refer [run-jetty]]
      [compojure.core :refer :all]
      [compojure.route :as route]
      [compojure.handler :as handler]
      [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
      [cemerick.friend        :as friend]
      [clj-time.core :as t]
      [clj-time.coerce :as c]      
      [hours.time :as time]
      [hours.layout :as layout]
      [hours.period :as period]      
      [hours.client :as client]
      [hours.prjct :as prjct]
      [hours.migrations :as migrations]
      [hours.security :as security]
      [environ.core :refer [env]])
    (:gen-class))

(def db-spec {:connection {:connection-uri (env :jdbc-database-url)}})

(defn start! [user project]
  (let [user-id (:workday-id user)
        period-id (period/start! db-spec user-id project)]
    (ring.util.response/redirect (str "/user/register/stop/" period-id))))

(defn stop! [user period-id]
  (let [user-id (:workday-id user)]
    (period/stop! db-spec user-id period-id)
    (ring.util.response/redirect "/user/register/start")))

(defn add-client! [user-id name]
  (client/add-client<! {:name name :user_id user-id} db-spec)
  (ring.util.response/redirect "/client/"))

(defn add-project! [client-id name]
  (prjct/add-project<! {:name name :client_id client-id} db-spec)
  (ring.util.response/redirect "/project/"))

(defn edit-period! [user-id id date start end project]
  (period/edit-period! db-spec user-id id date start end project)
  (ring.util.response/redirect "/user/register/start"))

(defn delete-period! [user-id id]
  (period/delete-period! db-spec user-id id)
  (ring.util.response/redirect "/user/register/start"))

(defn show-start [user-info]
  (layout/show-hours-page user-info "start" "" nil (period/by-user {:user_id (:workday-id user-info)} db-spec)))

(defn show-stop [user-info period-id]
  (layout/show-hours-page user-info "stop" "foo" period-id (period/by-user {:user_id (:workday-id user-info)} db-spec)))

(defn show-projects [user-info client-id]
  (layout/show-projects-page user-info
                             (prjct/user-client-projects {:user_id (:workday-id user-info) :client_id client-id} db-spec)))

(defroutes user-routes
  (GET "/" request (ring.util.response/redirect "/user/register/start"))
  (GET "/status" request (layout/show-status-page (security/user-info request) request))
  (GET "/week" request (layout/show-week-page (security/user-info request) (t/now)))
  (GET "/week/:date" [date :as r] (layout/show-week-page (security/user-info r) date))
  (GET "/register/stop/:period-id" [period-id :as r] (show-stop (security/user-info r) period-id))
  (GET "/register/start" request (show-start (security/user-info request)))
  (POST "/register/start" [project :as r] (start! (security/user-info r) project))
  (POST "/register/stop" [period-id :as r] (stop! (security/user-info r) period-id)))

(defroutes client-routes
  (GET "/" r (layout/show-clients-page (security/user-info r) (client/user-clients {:user_id (security/user-id r)} db-spec)))
  (GET "/:client-id/projects" [client-id :as r] (show-projects (security/user-info r) client-id))
  (GET "/add" r (layout/show-add-client-page (security/user-info r)))
  (POST "/add" [name :as r] (add-client! (security/user-id r) name)))

(defroutes project-routes
  (GET "/" r (layout/show-projects-page (security/user-info r) (prjct/user-projects {:user_id (security/user-id r)} db-spec)))
  (GET "/add/:client-id" [client-id :as r] (layout/show-add-project-page (security/user-info r) (first (client/user-client {:user_id (security/user-id r) :client_id client-id} db-spec))))
  (POST "/add/:client-id" [client-id name :as r] (add-project! client-id name)))

(defroutes period-routes
  (GET "/:id" [id :as r] (layout/show-edit-period-page (security/user-info r) (first (period/by-id {:user_id (security/user-id r) :id id} db-spec))))
  (POST "/:id" [id date start end project :as r] (edit-period! (security/user-id r) id date start end project))
  (GET "/:id/delete" [id :as r] (delete-period! (security/user-id r) id)))

(defroutes app-routes
  (GET "/" [] (layout/login-page))
  (GET "/status" request (layout/show-status-page {} request))
  (context "/user" request (friend/wrap-authorize user-routes #{security/user}))
  (context "/client" request (friend/wrap-authorize client-routes #{security/user}))
  (context "/project" request (friend/wrap-authorize project-routes #{security/user}))
  (context "/period" request (friend/wrap-authorize period-routes #{security/user}))    
  (friend/logout (ANY "/logout" request (ring.util.response/redirect "/")))
  (route/not-found "not found"))

(def app
  (-> #'app-routes
        (friend/authenticate (security/friend-config db-spec))
        (wrap-file "resources/public")
        handler/site))

(defn start-jetty  [port]
  (run-jetty #'app {:port port
                         :join? false}))

(defn -main []
  (migrations/migrate)
  (let [port (Integer. (or (env :port) "3000"))]
    (start-jetty port)))
