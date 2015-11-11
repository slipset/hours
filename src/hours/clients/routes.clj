(ns hours.clients.routes
(:require [compojure.core :refer :all]
          [hours
           [client :as client]
           [prjct :as prjct]]
          [hours.clients.layout :as layout]
          [hours.projects.layout :as project-layout]
          [ring.util.response :refer [header redirect response]]))

(defn add-client! [db user-id name]
  (client/add-client<! {:name name :user_id user-id} db)
  (ring.util.response/redirect "/client/"))

(defn show-projects [db user-id client-id]
  (project-layout/display-projects (prjct/user-client-projects {:user_id user-id :client_id client-id} db)))

(defroutes client-routes
  (GET "/" [db user-id] (response (layout/display-clients (client/user-clients {:user_id user-id} db))))
  (GET "/:client-id/projects" [db client-id user-id] (response (show-projects db user-id client-id)))
  (GET "/add" [] (response (layout/display-add-client)))
  (POST "/add" [db name user-id] (add-client! db user-id name)))
