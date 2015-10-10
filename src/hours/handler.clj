(ns hours.handler
    (:require
      [clj-time.core :as t]
      [clj-time.format :as f]
      [clojure.java.io :as io]
      [hiccup.core :refer [html]]
      [hiccup.page :refer [html5 include-js include-css]]
      [ring.adapter.jetty :refer [run-jetty]]
      [compojure.core :refer :all]
      [compojure.route :as route]
      [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(def hours (atom []))

(def current (atom {}))

(defn add-hours [m]
  (swap! hours conj m))

(def custom-formatter (f/formatter "dd/MM-yy"))

(defn prev-monday [dt]
  (let [mon 1
        today (t/day-of-week dt)]
    (if (= mon today)
      dt
      (t/minus dt (t/days (dec today)))))) 

(defn prev-week [mon]
  (t/minus mon (t/weeks 1)))

(defn next-week [mon]
  (t/plus mon (t/weeks 1)))

(defn add-days [dt i]
  (t/plus dt (t/days i)))

(defn week [dt]
  (map (partial add-days (prev-monday dt)) (range 0 7)))

(defn ->hour-mins [interval]
  (let [minutes (t/in-minutes interval)]
    (vector (int (/ minutes 60)) (rem minutes 60))))

(defn trunc-seconds [dt]
  (-> dt
      (t/minus (t/seconds (t/second dt)))
      (t/minus (t/millis (t/milli dt)))))

(defn format-interval [hours-mins]
  (apply format (cons "%02d:%02d" hours-mins)))

(defn find-total-by-day [[date periods]]
  {date {:start (:start (first periods)) :stop (:stop (last periods))}})

(defn by-date [hours]
  (let [days (group-by (fn [period] (f/unparse custom-formatter (:start period))) hours)]
    (mapcat find-total-by-day days)))

(defn display-week-chooser [mon]
  (let [prev (prev-week mon)
        next (next-week mon)]
    [:div "Uke " [:a {:href (str "/week/" (f/unparse (f/formatters :basic-date) prev )) } (.getWeekOfWeekyear prev) ]
     " "
     (.getWeekOfWeekyear mon)
     " "
     [:a {:href (str "/week/" (f/unparse (f/formatters :basic-date) next )) } (.getWeekOfWeekyear next) ]
     ]))

(defn display-week [week]
  [:div
   (display-week-chooser (first week))
   [:table
   [:tbody
    [:tr
     [:th "Date"]]
    (for [day week]
      [:tr
       [:td (f/unparse custom-formatter day)]])]]])

(defn display-hours [hours]
  [:table
   [:tbody
    [:tr
     [:th "Date"]
     [:th "Start"]
     [:th "End"]
     [:th "Total"]]
    (for [hour (by-date hours)]
      (let [start (:start (second hour) )
            stop (:stop (second hour) )
            diff (t/interval start stop)]
        [:tr
         [:td  (first hour)]
          [:td (f/unparse (f/formatters :hour-minute) start) ]
          [:td (f/unparse (f/formatters :hour-minute) stop) ]
          [:td (format-interval (->hour-mins diff))]
          [:td ]])
      )]])

(defn start-stop [action content]
  [:div [:form {:method "POST" :action (str "/register/" action) }
   (anti-forgery-field)
   [:tr
    [:td.submit {:colspan 2}
     [:input {:type "submit" :value action}]]]]
   content])


(defn page-template [content]
  (html5
   [:head
    [:title "hours"]]
   [:body
    [:h1 [:a {:href "/"} "hours"]]
    [:div.content content]]))

(defn start []
  (swap! current assoc :start (trunc-seconds (t/now)))
  (page-template (start-stop "stop" (display-hours @hours))))

(defn stop []
  (swap! current assoc :stop (trunc-seconds (t/now)))
  (add-hours @current)
  (reset! current {})
  (page-template  (start-stop "start" (display-hours @hours)) ))

(defroutes app-routes
  (GET "/" [] (page-template  (start-stop "start" (display-hours @hours))))
  (GET "/week" [] (page-template (display-week (week (t/now)))))
  (GET "/week/:date" [date] (page-template (display-week (week (f/parse (f/formatters :basic-date) date)))))
  (POST "/register/start" [] (start))
  (POST "/register/stop" [] (stop))  
  (route/not-found "not found"))

(def app
  (wrap-defaults #'app-routes site-defaults))


(defonce server (run-jetty #'app {:port 3000 :join? false}))
