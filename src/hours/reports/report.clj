(ns hours.reports.report
    (:require [yesql.core :refer [defqueries]]
              [hours.time :as time]
              [clj-time.coerce :as c]
              [clj-time.core :as t]
              [clj-time.format :as f]))

(defqueries "hours/reports/report.sql")

(defn by-week [db-spec user-id week-start week-end]
  (by-dates {:user_id user-id :period_start week-start :period_end week-end} db-spec))

(defn by-week-and-client [db-spec user-id client-id  week-start week-end]
  (by-client-dates {:user_id user-id :client_id client-id :period_start week-start :period_end week-end} db-spec))

(defn get-week-start [date]
  (time/prev-monday (if (= date ":this")
                      (time/trunc-hours (t/now))
                       (f/parse (f/formatters :basic-date) date))))
 
(defn group-project [period]
  {:client {:id (:client_id period)
            :name (:client_name period)}
   :project {:id (:project_id period)
             :name (:name period)
             :color (:color period)}})

(defn clean-up [entry]
  (-> entry
      (dissoc :workday_user_id_2 :workday_user_id :id_3 :id_2 :id)
      (clojure.set/rename-keys {:workday_client_id :client_id :workday_project_id :project_id :name_2 :client_name})))

(defn sum [acc period]
  (let [start (c/from-sql-time (:period_start period))
        stop (c/from-sql-time (:period_end period))
        minutes (t/in-minutes (t/interval start stop))]
    (+ acc minutes)))

(defn grand-total [report]
  (->> report
       (vals)
       (mapcat identity)
       (map :total)
       (reduce + 0)))

(defn get-client [line]
  {:id (get-in (first line) [:client :id]) :name (get-in (first line) [:client :name])})

(defn distinct-clients [report]
  (->> report
       (map get-client)
       (distinct)
       (cons {:id ":all" :name "All"})
       (sort-by :name)
       (into [])))

(defn has-data-for-day [days day]
  (keep (fn [d] (when (.equals day (:day d)) d)) days))

(defn add-missing-days [days]
  (let [week (time/week (:day (first days)))]
    (map #(let [d (has-data-for-day days %)]  (if (seq d) (first d) {:day % :total 0})) week)))

(defn summarize [project-day]
  (let [instant (first project-day)]
    {:total (reduce sum 0 project-day)
     :day (time/trunc-hours (c/from-sql-time (:period_start instant)))}))

(defn daily-totals [project-period]
  (->> project-period
       (group-by #(time/trunc-hours (c/from-sql-time (:period_start %))))
       (map (fn [[k v]] (summarize v)))
       (add-missing-days)))

(defn distinct-projects [report]
  (->> report
       (map (fn [r] {:id (get-in (first r) [:project :id])
                     :name (get-in (first r) [:project :name])
                     :color (get-in (first r) [:project :color])
                     :client (get-client r)}))
       (distinct)
       (sort-by :name)
       (into [])))

(defn day-totals [report]
  (if (seq report)
    (apply map (fn [& args] (reduce #(+ %1 (:total %2)) 0 args)) (vals report))
    '(0 0 0 0 0 0 0)))

(defn decorate
  ([[start end] report] (format start end nil report))
  ([[start end] client-id report] {:report report
                                   :grand-total (grand-total report)
                                   :client-id client-id
                                   :clients (distinct-clients report)
                                   :projects (distinct-projects report)
                                   :period-start start
                                   :period-end end
                                   :day-totals (day-totals report)}))

(defn weekly
  ([db-spec user-id [week-start week-end]] 
   (by-week db-spec user-id (c/to-sql-time week-start) (c/to-sql-time week-end)))
  ([db-spec user-id [week-start week-end] client-id]
   (by-week-and-client db-spec user-id client-id (c/to-sql-time week-start) (c/to-sql-time week-end))))


(defn get-weekly-report [db-spec user-id client-id date]
  (let [week (time/week-period (get-week-start date))
        report (if (= client-id ":all")
                 (weekly db-spec user-id week)
                 (weekly db-spec user-id week client-id))]
    (->> report
         (map clean-up)
         (sort-by :period-start)
         (group-by group-project)
         (map (fn [[k v]] [k (daily-totals v)]))
         (into {})
         (decorate week client-id))))
