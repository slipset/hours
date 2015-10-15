(ns hours.period
    (:require [yesql.core :refer [defqueries]]
              [hours.prjct :as prjct]
              [hours.time :as time]              
              [clj-time.coerce :as c]
              [clj-time.core :as t]))

(defqueries "hours/period.sql")

(defn start! [db-spec user-id project]
  (let [project-id (:id (first (prjct/by-name {:name project :user_id user-id} db-spec)))]
    (:id (start<! {:user_id user-id :project_id project-id :start (c/to-sql-time (t/now))} db-spec))))

(defn stop! [db-spec user-id period-id]
  (end! {:id period-id :end (c/to-sql-time (t/now)) :user_id user-id} db-spec))

(defn edit-period! [db-spec user-id id date start end project]
  (let [start (time/->dt date start)
        end (time/->dt date end)
        project-id (:id (first (prjct/by-name {:name project :user_id user-id} db-spec)))]
    (update! {:id id
              :project_id project-id
              :user_id user-id
              :start (c/to-sql-time start)
              :end (c/to-sql-time end)} db-spec)))

