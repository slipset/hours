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


(defn ->hour-mins [interval]
  (let [minutes (t/in-minutes interval)]
    (vector (int (/ minutes 60)) (rem minutes 60))))

(defn format-interval [hours-mins]
  (apply format (cons "%02d:%02d" hours-mins)))

(defn find-total-by-day [[date periods]]
  {date {:start (:start (first periods)) :stop (:stop (last periods))}})

(defn by-date [hours]
  (let [days (group-by (fn [period] (f/unparse custom-formatter (:start period))) hours)]
    (mapcat find-total-by-day days)))

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

(defn page-template [action content]
  (html5
   [:head
    [:title "hours"]]
   [:body
    [:h1 [:a {:href "/"} "hours"]]
    [:form {:method "POST" :action (str "/register/" action) }
     (anti-forgery-field)
     [:tr
      [:td.submit {:colspan 2}
       [:input {:type "submit" :value action}]]]]
    content]))

(defn trunc-seconds [dt]
  (-> dt
      (t/minus (t/seconds (t/second dt)))
      (t/minus (t/millis (t/milli dt)))))

(defn start []
  (swap! current assoc :start (trunc-seconds (t/now)))
  (page-template "stop" (display-hours @hours)))

(defn stop []
  (swap! current assoc :stop (trunc-seconds (t/now)))
  (add-hours @current)
  (reset! current {})
  (page-template "start" (display-hours @hours)))

(defroutes app-routes
  (GET "/" [] (page-template "start" ""))
  (POST "/register/start" [] (start))
  (POST "/register/stop" [] (stop))  
  (route/not-found "Not Found"))

(def app
  (wrap-defaults #'app-routes site-defaults))


(defonce server (run-jetty #'app {:port 3000 :join? false}))
