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
      [hours.report :as report]
      [hours.migrations :as migrations]
      [hours.security :as security]
      [environ.core :refer [env]])
    (:gen-class))

(def db-spec {:connection {:connection-uri (env :jdbc-database-url)}})

(defn start! [user-id description project date]
  (let [period-id (period/start! db-spec user-id description project
                                 (time/add-now (time/add-this-year (f/parse (f/formatter "dd/MM") date))))]
    (ring.util.response/redirect (str "/user/register/stop/" period-id))))

(defn stop! [user-id period-id]
  (period/stop! db-spec user-id period-id (t/now))
  (ring.util.response/redirect "/user/register/start"))

(defn add-client! [user-id name]
  (client/add-client<! {:name name :user_id user-id} db-spec)
  (ring.util.response/redirect "/client/"))

(defn add-project! [client-id name]
  (prjct/add-project<! {:name name :client_id client-id} db-spec)
  (ring.util.response/redirect "/project/"))

(defn edit-period! [user-id id date start end description project]
  (period/edit-period! db-spec user-id id date start end description project)
  (ring.util.response/redirect "/user/register/start"))

(defn delete-period! [user-id id]
  (period/delete-period! db-spec user-id id)
  (ring.util.response/redirect "/user/register/start"))

(defn show-start [user-id]
  (->> (layout/display-hours (period/by-user {:user_id user-id} db-spec))
       (layout/start-stop "start" "" nil nil)))

(defn show-stop [user-id period-id description project-name]
  (->> (layout/display-hours (period/by-user {:user_id user-id} db-spec))
       (layout/start-stop "stop" period-id description project-name)))

(defn show-start-stop
  ([user-id]
   (let [unstopped (first (period/find-unstopped db-spec user-id))]
    (if (seq unstopped)
      (show-stop user-id (:id unstopped) (:description unstopped) (:name unstopped))
      (show-start user-id))))
  ([user-id period-id]
   (let [unstopped (first (period/by-id {:id period-id :user_id user-id} db-spec))]
     (show-stop user-id (:id unstopped) (:description unstopped) (:name unstopped) ))))

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

(defn text-html [resp]
  (-> (response resp)
      (header "Content-type" "text/html; charset=utf-8")))

(defroutes user-routes
  (GET "/" request (redirect "/user/register/start"))
  (GET "/status" request (layout/display-status-page request))
  (GET "/register/stop/:period-id" [period-id user-id] (text-html (show-start-stop user-id period-id)))
  (GET "/register/start" [user-id] (text-html (show-start-stop user-id)))
  (POST "/register/start" [user-id project description date] (start! user-id description project date))
  (POST "/register/stop" [period-id user-id] (stop! user-id period-id)))

(defroutes client-routes
  (GET "/" [user-id] (text-html (layout/display-clients (client/user-clients {:user_id user-id} db-spec))))
  (GET "/:client-id/projects" [client-id user-id] (text-html (show-projects user-id client-id)))
  (GET "/add" [] (text-html (layout/display-add-client)))
  (POST "/add" [name user-id] (add-client! user-id name)))

(defroutes project-routes
  (GET "/" [user-id] (text-html (layout/display-projects (prjct/user-projects {:user_id user-id} db-spec))))
  (GET "/add/:client-id" [client-id user-id] (text-html (layout/display-add-project (first (client/user-client {:user_id user-id :client_id client-id} db-spec)))))
  (POST "/add/:client-id" [client-id name] (add-project! client-id name)))

(defroutes period-routes
  (GET "/:id" [id user-id] (text-html (layout/display-edit-period (first (period/by-id {:user_id user-id :id id} db-spec)))))
  (POST "/:id" [user-id id date start end project description]  (edit-period! user-id id date start end description project))
  (GET "/:id/delete" [id user-id] (delete-period! user-id id)))

(defroutes report-routes
  (GET "/by-week" r (redirect "/report/by-week/:all/:this"))
  (GET "/by-week/:client-id/:date" [client-id date user-id] (text-html (layout/display-report
                                                                        client-id
                                                                        (get-week-start date)
                                                                        (get-week-report user-id client-id
                                                                                         (get-week-start date))))))
(defroutes app-routes
  (GET "/" [user-id] (if (empty? user-id) (text-html (layout/render-login)) (redirect "/user")))
  (GET "/status" request (layout/display-status-page request))
  (context "/user" request (friend/wrap-authorize user-routes #{security/user}))
  (context "/client" request (friend/wrap-authorize client-routes #{security/user}))
  (context "/project" request (friend/wrap-authorize project-routes #{security/user}))
  (context "/period" request (friend/wrap-authorize period-routes #{security/user}))
  (context "/report" request (friend/wrap-authorize report-routes #{security/user}))      
  (friend/logout (ANY "/logout" request (ring.util.response/redirect "/")))
  (rfn request (-> {:body  (layout/display-not-found request)} (status 404))))


(defn wrap-add-user-id [handler uid-param]
  (fn [request]
    (handler (assoc-in request [:params uid-param] (security/user-id request)))))

(defn wrap-page-template [handler]
  (fn [request]
    (update-in (handler request) [:body] (layout/get-html5 (security/user-info request)))))

(def app
  (-> #'app-routes
        (wrap-page-template)
        (wrap-add-user-id :user-id)
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
