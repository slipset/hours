(ns hours.handler
    (:require
      [clojure.java.jdbc :as sql]
      [ring.middleware.file :refer [wrap-file]]
      [ring.adapter.jetty :refer [run-jetty]]
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
      [hours.report :as report]
      [hours.migrations :as migrations]
      [hours.security :as security]
      [environ.core :refer [env]])
    (:gen-class))

(def db-spec {:connection {:connection-uri (env :jdbc-database-url)}})

(defn start! [user project date]
  (let [user-id (:workday-id user)
        period-id (period/start! db-spec user-id project
                                 (time/add-now (time/add-this-year (f/parse (f/formatter "dd/MM") date))))]
    (ring.util.response/redirect (str "/user/register/stop/" period-id))))

(defn stop! [user period-id]
  (let [user-id (:workday-id user)]
    (period/stop! db-spec user-id period-id (t/now))
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

(defn show-start [user-id]
  (layout/start-stop "start" "" nil (period/by-user {:user_id user-id} db-spec)))

(defn show-stop [user-id period-id project-name]
  (layout/start-stop "stop" project-name period-id (period/by-user {:user_id user-id} db-spec)))

(defn show-start-stop
  ([user-id]
   (let [unstopped (first (period/find-unstopped db-spec user-id))]
    (if (seq unstopped)
      (show-stop user-id (:id unstopped) (:name_2 unstopped))
      (show-start user-id))))
  ([user-id period-id]
   (let [unstopped (first (period/by-id {:id period-id :user_id user-id} db-spec))]
     (show-stop user-id (:id unstopped) (:name_2 unstopped)))))

(defn show-projects [user-id client-id]
  (layout/display-projects (prjct/user-client-projects {:user_id user-id :client_id client-id} db-spec)))

(defn get-week-start [date]
  (time/prev-monday (if (= date ":this")
                      (time/trunc-hours  (t/now))
                       (f/parse (f/formatters :basic-date) date))))
 
(defn get-week-report [user-id client-id date]
  (if (= client-id ":all")
    (report/weekly db-spec
                   user-id
                   date
                   report/group-by-date-project)
    (report/weekly-by-client db-spec
                             user-id
                             client-id
                             date
                             report/group-by-date-project)))
    
(defroutes user-routes
  (GET "/" request (ring.util.response/redirect "/user/register/start"))
  (GET "/status" request (layout/display-status-page request))
  (GET "/register/stop/:period-id" [period-id :as r] (show-start-stop (security/user-id r) period-id))
  (GET "/register/start" request (show-start-stop (security/user-info request)))
  (POST "/register/start" [project date :as r] (start! (security/user-id r) project date))
  (POST "/register/stop" [period-id :as r] (stop! (security/user-id r) period-id)))

(defroutes client-routes
  (GET "/" r (layout/display-clients (client/user-clients {:user_id (security/user-id r)} db-spec)))
  (GET "/:client-id/projects" [client-id :as r] (show-projects (security/user-id r) client-id))
  (GET "/add" r (layout/display-add-client))
  (POST "/add" [name :as r] (add-client! (security/user-id r) name)))

(defroutes project-routes
  (GET "/" r (layout/display-projects (prjct/user-projects {:user_id (security/user-id r)} db-spec)))
  (GET "/add/:client-id" [client-id :as r] (layout/display-add-project (first (client/user-client {:user_id (security/user-id r) :client_id client-id} db-spec))))
  (POST "/add/:client-id" [client-id name :as r] (add-project! client-id name)))

(defroutes period-routes
  (GET "/:id" [id :as r] (layout/display-edit-period (first (period/by-id {:user_id (security/user-id r) :id id} db-spec))))
  (POST "/:id" [id date start end project :as r] (edit-period! (security/user-id r) id date start end project))
  (GET "/:id/delete" [id :as r] (delete-period! (security/user-id r) id)))

(defroutes report-routes
  (GET "/by-week" r (ring.util.response/redirect "/report/by-week/:all/:this"))
  (GET "/by-week/:client-id/:date" [client-id date :as r] (layout/display-report client-id
                                                                                 (get-week-start date)
                                                                                 (get-week-report (security/user-id r)
                                                                                                  client-id
                                                                                                  (get-week-start date)))))

(defroutes app-routes
  (GET "/" [] (layout/render-login))
  (GET "/status" request (layout/display-status-page request))
  (context "/user" request (friend/wrap-authorize user-routes #{security/user}))
  (context "/client" request (friend/wrap-authorize client-routes #{security/user}))
  (context "/project" request (friend/wrap-authorize project-routes #{security/user}))
  (context "/period" request (friend/wrap-authorize period-routes #{security/user}))
  (context "/report" request (friend/wrap-authorize report-routes #{security/user}))      
  (friend/logout (ANY "/logout" request (ring.util.response/redirect "/")))
  (rfn request (-> {:body  (layout/display-not-found request)} (status 404))))

(defn wrap-page-template [handler]
  (fn [request]
    (let [user (security/user request)
          response (handler request)]
      (update-in response [:body] (layout/get-html5 user)))))

(def app
  (-> #'app-routes
        (friend/authenticate (security/friend-config db-spec))
        (wrap-page-template)
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
