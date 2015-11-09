(ns hours.periods.routes
    (:require
      [compojure.core :refer :all]
      [ring.util.response :refer [response redirect header]]
      [hours.periods.layout :as layout]
      [hours.prjct :as prjct]
      [hours.period :as period]))

(defn edit-period! [db user-id id date start end description project-id]
  (period/edit-period! db user-id id date start end description project-id)
  (ring.util.response/redirect "/user/register/start"))

(defn delete-period! [db user-id id]
  (period/delete-period! db user-id id)
  (ring.util.response/redirect "/user/register/start"))

(defn text-html [resp]
  (-> (response resp)
      (header "Content-type" "text/html; charset=utf-8")))

(defroutes period-routes
  (GET "/:id" [db user-id id] (text-html (layout/display-edit-period (first (period/by-id {:user_id user-id :id id} db))
                                                                     (prjct/user-projects {:user_id user-id } db))))
  (POST "/:id" [db user-id id date start end project-id description]  (edit-period! db user-id id date start end description project-id))
  (GET "/:id/delete" [db id user-id] (delete-period! db user-id id)))
