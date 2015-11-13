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
                                 (time/add-now (f/parse time/custom-formatter date)))]
    (ring.util.response/redirect (str "/user/register/stop/" period-id))))

(defn stop! [db user-id period-id]
  (period/stop! db user-id period-id (t/now))
  (ring.util.response/redirect "/user/register/start"))

(defn get-stuff [db user-id]
  {:projects (prjct/user-projects-by-importance {:user_id user-id} db)
   :hours (period/by-user {:user_id user-id} db)
   :now (t/now)})

(defn show-stop [db user-id period-id]
  (let [unstopped (first (period/by-id {:id period-id :user_id user-id} db))]
    (layout/display-registration (get-stuff db user-id) unstopped)))

(defn show-start [db user-id]
  (let [unstopped (first (period/find-unstopped db user-id))]
    (if (seq unstopped)
      (ring.util.response/redirect (str "/user/register/stop/" (:id unstopped)))
      (response (layout/display-registration (get-stuff db user-id))))))

(defroutes user-routes
  (GET "/" request (redirect "/user/register/start"))
  (GET "/register/stop/:period-id" [user-id db user-id period-id] (response (show-stop db user-id period-id)))
  (GET "/register/start" [user-id db] (show-start db user-id))
  (POST "/register/start" [user-id db project-id description date] (start! db user-id description project-id date))
  (POST "/register/stop" [user-id db period-id] (stop! db user-id period-id)))
