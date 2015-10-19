(ns hours.report
    (:require [yesql.core :refer [defqueries]]
              [hours.time :as time]
              [clj-time.coerce :as c]))

(defqueries "hours/report.sql")

(defn by-week [db-spec user-id week-start week-end]
  (by-dates {:user_id user-id :period_start week-start :period_end week-end} db-spec))

(defn by-week-and-client [db-spec user-id client-id  week-start week-end]
  (by-client-dates {:user_id user-id :client_id client-id :period_start week-start :period_end week-end} db-spec))

(defn group-by-date-project [period]
  {:period-start (time/trunc-hours (c/from-sql-time (:period_start period)))
   :client {:id (:id_3 period)
            :name (:name_3 period)}
   :project {:id (:id_2 period)
             :name (:name_2 period)}
   })

(defn weekly [db-spec user-id date group-function]
  (let [[week-start week-end] (time/week-period date)]
    (->> (by-week db-spec user-id (c/to-sql-time week-start) (c/to-sql-time week-end))
         (group-by group-function)
         (sort-by :period-start))))

(defn weekly-by-client [db-spec user-id client-id date group-function]
   (let [[week-start week-end] (time/week-period date)]
     (->> (by-week-and-client db-spec user-id client-id (c/to-sql-time week-start) (c/to-sql-time week-end))
          (group-by group-function)
          (sort-by :period-start))))
