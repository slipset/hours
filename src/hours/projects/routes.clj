(ns hours.projects.routes
    (:require
      [ring.util.response :refer [response redirect header]]
      [compojure.core :refer :all]
      [hours.prjct :as prjct]
      [hours.client :as client]      
      [hours.projects.layout :as layout]))

(defn add-project! [db client-id name color]
  (prjct/add<! {:name name :client_id client-id :color color} db)
  (ring.util.response/redirect "/project/"))

(defn edit-project! [db project-id name color]
  (prjct/update! {:name name :project_id project-id :color color} db)
  (ring.util.response/redirect "/project/"))

(defn text-html [resp]
  (-> (response resp)
      (header "Content-type" "text/html; charset=utf-8")))

(defroutes project-routes
  (GET "/" [db user-id] (text-html (layout/display-projects (prjct/user-projects {:user_id user-id} db))))
  (GET "/add/:client-id" [user-id db client-id] (text-html (layout/display-add-project (first (client/user-client {:user_id user-id :client_id client-id} db)))))
  (GET "/edit/:project-id" [user-id project-id db] (text-html (layout/display-edit-project (first (prjct/user-project {:user_id user-id :project_id project-id} db)))))
  (POST "/edit/:project-id" [project-id db name color] (edit-project! db project-id name color))
  (POST "/add/:client-id" [client-id db name color] (add-project! db client-id name color)))
