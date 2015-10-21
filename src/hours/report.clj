(ns hours.report
    (:require [yesql.core :refer [defqueries]]
              [hours.time :as time]
              [clj-time.coerce :as c]
              [clj-time.core :as t]
              [clj-time.format :as f]))

(defqueries "hours/report.sql")



(defn by-week [db-spec user-id week-start week-end]
  (by-dates {:user_id user-id :period_start week-start :period_end week-end} db-spec))

(defn by-week-and-client [db-spec user-id client-id  week-start week-end]
  (by-client-dates {:user_id user-id :client_id client-id :period_start week-start :period_end week-end} db-spec))


(defn get-week-start [date]
  (time/prev-monday (if (= date ":this")
                      (time/trunc-hours (t/now))
                       (f/parse (f/formatters :basic-date) date))))
 
(defn group-by-date-project [period]
  {:client {:id (:id_3 period)
            :name (:name_2 period)}
   :project {:id (:id_2 period)
             :name (:name period)}
   :period-start (time/trunc-hours (c/from-sql-time (:period_start period)))})

(defn sum [acc period]
  (let [start (c/from-sql-time (:period_start period))
        stop (c/from-sql-time (:period_end period))
        minutes (t/in-minutes (t/interval start stop))]
    (+ acc minutes)))

(defn grand-total [report]
  (->> report
       (mapcat (fn [[_ val]] val))
       (reduce sum 0)))

(defn get-client [line]
  {:id (get-in (first line) [:client :id]) :name (get-in (first line) [:client :name])})

(defn distinct-clients [report]
  (->> report
       (map get-client)
       (distinct)
       (cons {:id ":all" :name "All"})
       (sort-by :name)
       (into [])))

(defn distinct-projects [report]
  (->> report
       (map (fn [r] {:id (get-in (first r) [:project :id]) :name (get-in (first r) [:project :name])
                     :client (get-client r)}))
       (distinct)
       (sort-by :name)
       (into [])))


(defn decorate
  ([start end report] (format start end nil report))
  ([start end client-id report] {:report report
                                 :grand-total (grand-total report)
                                 :client-id client-id
                                 :clients (distinct-clients report)
                                 :projects (distinct-projects report)
                                 :period-start start
                                 :period-end end}))

(defn weekly
  ([db-spec user-id week-start week-end] 
   (by-week db-spec user-id (c/to-sql-time week-start) (c/to-sql-time week-end)))
  ([db-spec user-id week-start week-end client-id]
   (by-week-and-client db-spec user-id client-id (c/to-sql-time week-start) (c/to-sql-time week-end))))

(defn get-weekly-report [db-spec user-id client-id date]
  (let [[week-start week-end] (time/week-period (get-week-start date))
        report (if (= client-id ":all")
                 (weekly db-spec user-id week-start week-end )
                 (weekly db-spec user-id week-start week-end client-id))]
    (->> report
         (sort-by :period-start)
         (group-by group-by-date-project)
         (decorate week-start week-end client-id))))
