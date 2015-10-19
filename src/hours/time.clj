(ns hours.time
    (require  [clj-time.core :as t]
              [clj-time.format :as f]))

(def custom-formatter (f/formatter "dd/MM-yy"))

(defn trunc-seconds [dt]
  (-> dt
      (t/minus (t/seconds (t/second dt)))
      (t/minus (t/millis (t/milli dt)))))

(defn trunc-hours [dt]
  (-> (trunc-seconds dt)
      (t/minus (t/minutes (t/minute dt)))
      (t/minus (t/hours (t/hour dt)))))

(defn prev-monday [dt]
  (let [mon 1
        today (t/day-of-week dt)]
    (if (= mon today)
      dt
      (t/minus dt (t/days (dec today)))))) 

(defn week-period [dt]
  (let [mon (trunc-hours (prev-monday dt))]
    [mon (-> (t/plus mon (t/days 7))
             (t/minus (t/millis 1)))]))

(defn prev-week [mon]
  (t/minus mon (t/weeks 1)))

(defn today-at [[hour min]]
  (-> (t/today-at 00 00)
      (t/plus (t/hours (Integer.  hour)))
      (t/plus (t/minutes (Integer.  min)))))

(defn next-week [mon]
  (t/plus mon (t/weeks 1)))

(defn add-days [dt i]
  (t/plus dt (t/days i)))

(defn format-interval [hours-mins]
  (apply format (cons "%02d:%02d" hours-mins)))

(defn format-minutes [minutes]
  (format-interval (vector (int (/ minutes 60)) (rem minutes 60))))

(defn week [dt]
  (map (partial add-days (prev-monday dt)) (range 0 7)))

(defn ->hour-mins [interval]
  (let [minutes (t/in-minutes interval)]
    (vector (int (/ minutes 60)) (rem minutes 60))))

(defn ->dt
  ([date hour] (->dt date hour (t/time-zone-for-id "CET")))
  ([date hour tz] 
   (let [fmt (f/with-zone (f/formatter "dd/MM-yyHH:mm") tz)]
     (f/parse fmt (str date hour)))))

(defn find-total-by-day [[date periods]]
  {date {:start (:start (first periods)) :stop (:stop (last periods))}})

(defn by-date [hours]
  (let [days (group-by (fn [period] (vector (f/unparse custom-formatter (:start period)) (:project period))) hours)]
    (mapcat find-total-by-day days)))

(defn ->date-str [dt]
  (f/unparse custom-formatter dt))

(defn ->date-dd.mm [dt]
  (f/unparse (f/formatter "dd/MM") dt))

(defn ->hh:mm-str
  ([dt] (->hh:mm-str dt (t/time-zone-for-id "CET")))
  ([dt tz] (f/unparse (f/with-zone  (f/formatters :hour-minute) tz) dt)))

(defn add-this-year [dt]
  (->> (t/now)
       (t/year)
       (- 2000)
       (* -1)
       (t/years)
       (t/plus dt)))

(defn add-now [dt]
  (let [now (t/now)
        mins (t/minute now)
        hours (t/hour now)]
    (-> dt
        (t/plus (t/minutes mins))
        (t/plus (t/hours hours)))))
