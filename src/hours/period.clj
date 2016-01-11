(ns hours.period
    (:require [yesql.core :refer [defqueries]]
              [hours.prjct :as prjct]
              [hours.time :as time]
              [clj-time.coerce :as c]
              [clj-time.core :as t]))

(defqueries "hours/period.sql")

(defn start! [db-spec user-id description project-id date]
  (:id (start<! {:user_id user-id :description description :project_id project-id :start (c/to-sql-time date)} db-spec)))

(defn stop! [db-spec user-id period-id end]
  (end! {:id period-id :end (c/to-sql-time end) :user_id user-id} db-spec))

(defn delete-period! [db-spec user-id id]
  (delete! {:id id :user_id user-id} db-spec))

(defn edit-period! [db-spec user-id id date start end description project-id]
  (let [start (c/to-sql-time (time/->dt date start))
        end (if (empty? end)
              nil
              (c/to-sql-time (time/->dt date end)))]
    (update! {:id id
              :description description
              :project_id project-id
              :user_id user-id
              :start start
              :end end} db-spec)))

(defn find-unstopped [db-spec user-id]
  (unstopped {:user_id user-id} db-spec))
