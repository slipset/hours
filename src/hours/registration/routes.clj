(ns hours.registration.routes
    (:require
      [ring.util.response :refer [response redirect header]]
      [compojure.core :refer :all]
      [hours.registration.layout :as layout]
      [hours.period :as period]
      [hours.time :as time]
      [hours.prjct :as prjct]
      [hours.period :as period]
      [clj-time.format :as f]
      [clj-time.core :as t]))

(defn start! [db user-id description project-id date]
  (let [period-id (period/start! db user-id description project-id
                                 (time/add-now (time/add-this-year (f/parse (f/formatter "dd/MM") date))))]
    (ring.util.response/redirect (str "/user/register/stop/" period-id))))

(defn stop! [db user-id period-id]
  (period/stop! db user-id period-id (t/now))
  (ring.util.response/redirect "/user/register/start"))

(defn show-start [hours projects]
  (->> (layout/display-hours hours)
       (layout/start-stop "start" "" nil nil projects)))

(defn show-stop [hours period-id description project-name projects]
  (->> (layout/display-hours hours)
       (layout/start-stop "stop" period-id description project-name projects)))

(defn show-start-stop
  ([db user-id]
   (let [unstopped (first (period/find-unstopped db user-id))
         projects (prjct/user-projects {:user_id user-id} db)]
    (if (seq unstopped)
      (show-stop (period/by-user {:user_id user-id} db)
                 (:id unstopped) (:description unstopped) (:name unstopped) projects)
      (show-start (period/by-user {:user_id user-id} db) projects))))
  ([db user-id period-id]
   (let [unstopped (first (period/by-id {:id period-id :user_id user-id} db))
         projects (prjct/user-projects {:user_id user-id} db)]
     (show-stop (period/by-user {:user_id user-id} db)
                (:id unstopped) (:description unstopped) (:name unstopped) projects))))


(defroutes user-routes
  (GET "/" request (redirect "/user/register/start"))
;  (GET "/status" request (layout/display-status-page request))
  (GET "/register/stop/:period-id" [user-id db user-id period-id] (response (show-start-stop db user-id period-id)))
  (GET "/register/start" [user-id db] (response (show-start-stop db user-id)))
  (POST "/register/start" [user-id db project-id description date] (start! db user-id description project-id date))
  (POST "/register/stop" [user-id db period-id] (stop! db user-id period-id)))

