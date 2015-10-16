(ns hours.time
    (require  [clj-time.core :as t]
              [clj-time.format :as f]))

(def custom-formatter (f/formatter "dd/MM-yy"))

(defn prev-monday [dt]
  (let [mon 1
        today (t/day-of-week dt)]
    (if (= mon today)
      dt
      (t/minus dt (t/days (dec today)))))) 

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

(defn week [dt]
  (map (partial add-days (prev-monday dt)) (range 0 7)))

(defn ->hour-mins [interval]
  (let [minutes (t/in-minutes interval)]
    (vector (int (/ minutes 60)) (rem minutes 60))))

(defn ->dt [date hour]
  (let [fmt (f/formatter "dd/MM-yyHH:mm")]
    (f/parse fmt (str date hour))))

(defn trunc-seconds [dt]
  (-> dt
      (t/minus (t/seconds (t/second dt)))
      (t/minus (t/millis (t/milli dt)))))

(defn format-interval [hours-mins]
  (apply format (cons "%02d:%02d" hours-mins)))

(defn find-total-by-day [[date periods]]
  {date {:start (:start (first periods)) :stop (:stop (last periods))}})

(defn by-date [hours]
  (let [days (group-by (fn [period] (vector (f/unparse custom-formatter (:start period)) (:project period))) hours)]
    (mapcat find-total-by-day days)))

(defn ->date-str [dt]
  (f/unparse custom-formatter dt))

(defn ->date-dd.mm [dt]
  (f/unparse (f/formatter "dd/MM") dt))

(defn ->hh:mm-str [dt]
  (f/unparse (f/formatters :hour-minute) dt))

(defn add-this-year [dt]
  (->> (t/now)
       (t/year)
       (- 2000)
       (* -1)
       (t/years)
       (t/plus dt)))
