(ns hours.report
    (:require [yesql.core :refer [defqueries]]
              [hours.time :as time]
              [clj-time.coerce :as c]))

(defqueries "hours/report.sql")

(defn by-week [db-spec user-id week-start week-end]
  (by-dates {:user_id user-id :period_start week-start :period_end week-end} db-spec))

(defn group-by-date-project [period]
  [(time/trunc-hours (c/from-sql-time (:period_start period))) (str  (:name_3 period) "/" (:name_2 period))])

(defn weekly-report [db-spec user-id date group-function]
  (let [[week-start week-end] (time/week-period date)]
    (->> (by-week db-spec user-id (c/to-sql-time week-start) (c/to-sql-time week-end))
         (group-by group-function)
         (sort-by first))))
