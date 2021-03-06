(ns hours.time
    (require  [clj-time.core :as t]
              [clj-time.format :as f]))

(def custom-formatter (f/formatter "yyyy-MM-dd"))

(def display-date-formatter (f/formatter "MMM dd"))

(defn basic-date [dt]
  (f/unparse (f/formatters :basic-date) dt))

(defn basic-day [dt]
  (f/unparse (f/formatter "E MMM dd" ) dt))

(defn trunc-seconds [dt]
  (some-> dt
          (t/minus (t/seconds (t/second dt)))
          (t/minus (t/millis (t/milli dt)))))

(defn trunc-hours [dt]
  (some-> dt
          (trunc-seconds)
          (t/minus (t/minutes (t/minute dt)))
          (t/minus (t/hours (t/hour dt)))))

(defn prev-monday [dt]
  (trunc-hours (let [mon 1
                     today (t/day-of-week dt)]
                 (if (= mon today)
                   dt
                   (t/minus dt (t/days (dec today)))))))

(defn trunc-days [dt]
  (t/date-time (t/year dt) (t/month dt)))

(defn week-period [dt]
  (let [mon (trunc-hours (prev-monday dt))]
    [mon (-> mon
             (t/plus (t/days 7))
             (t/minus (t/millis 1)))]))

(defn month-period [dt]
  (let [start (trunc-days dt)]
    [start (-> start
               (t/plus (t/months 1))
               (t/minus (t/millis 1)))]))

(defn prev-week [mon]
  (t/minus mon (t/weeks 1)))

(defn prev-month [mon]
  (t/minus mon (t/months 1)))

(defn next-month [mon]
  (t/plus mon (t/months 1)))

(defn today-at [[hour min]]
  (-> (t/today-at 00 00)
      (t/plus (t/hours (Integer.  hour)))
      (t/plus (t/minutes (Integer.  min)))))

(defn next-week [mon]
  (t/plus mon (t/weeks 1)))

(defn add-days [dt i]
  (t/plus dt (t/days i)))

(defn all-days [start end]
  (map (partial add-days start) (range 0 (inc  (t/in-days (t/interval start end))))))

(defn format-with-tz
  ([dt format] (format-with-tz dt format (t/time-zone-for-id "CET")))
  ([dt format tz-id]
   (f/unparse (f/with-zone format tz-id) dt)))

(defn format-interval [hours-mins]
  (apply format (cons "%02d:%02d" hours-mins)))

(defn format-minutes [minutes]
  (format-interval (vector (int (/ minutes 60)) (rem minutes 60))))

(defn week [dt]
  (map (partial add-days (prev-monday dt)) (range 0 7)))

(defn month [dt]
  (apply all-days (month-period dt)))

(defn ->hour-mins [interval]
  (let [minutes (t/in-minutes interval)]
    (vector (int (/ minutes 60)) (rem minutes 60))))

(defn ->dt
  ([date hour] (->dt date hour (t/time-zone-for-id "CET")))
  ([date hour tz] 
   (let [fmt (f/with-zone (f/formatter "yyyy-MM-ddHH:mm") tz)]
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
