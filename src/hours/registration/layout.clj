(ns hours.registration.layout
    (:require
      [clj-time.core :as t]
      [clj-time.format :as f]
      [clj-time.coerce :as c]
      [hours.time :as time]))

(defn display-project [project client color]
  [:h5 {:style "margin-top: 0px; margin-bottom: 0px;"}
   [:span {:style (str "padding:2px;background-color:#" color)} project]
   "&nbsp;" [:small client]])

(defn display-hours [hours]
  [:table.table
   [:tbody
    [:tr
     [:th "Date"]
     [:th "Project"]
     [:th "Period"]
     [:th.text-right "Total"]
     [:th "&nbsp;"]]
    (for [hour hours]
      (let [start (c/from-sql-time (:period_start hour)) 
            stop (c/from-sql-time (:period_end hour))
            diff (t/interval start stop)]
        [:tr
         [:td (time/format-with-tz start time/display-date-formatter)]
         [:td [:div (display-project (:name hour) (:name_2 hour) (:color hour))] [:span.small (:description hour)]]
         [:td (when start (time/->hh:mm-str start) ) " - "
          (when stop
            (time/->hh:mm-str stop))]
         [:td.text-right (time/format-interval (time/->hour-mins diff))]
         [:td.text-right [:a {:href (str "/period/" (:id hour))} "edit"] " | "
          [:a {:href (str "/period/" (:id hour) "/delete")} "delete"]]]))]])

(defn stop [{:keys [id name description]}]
  (list [:input {:type "hidden" :name "period-id" :value (str id)}]
        [:div.input-group-btn {:style "width: 30%"}
         [:input.form-control {:type "text" :name "description" :value description :readonly "readonly"}]]
        [:input.form-control {:type "text" :name "project" :value name :readonly "readonly"}]
        [:span.input-group-btn [:button.btn.btn-danger {:type "submit" :value "stop"} "stop"]]))

(defn start [projects now]
  (list [:div.input-group-btn
         [:input.form-control {:type "date" :style "width: 10em" :name "date"
                               :value (time/->date-str now) :max (time/->date-str now)} ]]
        [:div.input-group-btn {:style "width: 30%"}
         [:input.form-control {:type "text" :name "description" :placeholder "What are you doing?"}]]
        [:select.form-control {:name "project-id"}
         (map (fn [p] [:option {:value (:project_id p)} (str (:project_name p) " - " (:client_name p))]) projects)]
        [:span.input-group-btn [:button.btn.btn-success {:type "submit" :value "start"} "start"]]))

(defn display-form [action content]
  [:form {:method "POST" :action (str "/user/register/" (name action)) }
    [:style " ::-webkit-inner-spin-button { display: none; }"]
    [:div.input-group
     content]])

(defn display-content [content form]
  [:div.row form content])

(defn do-display [action hours content]
  (->> content
       (display-form action)
       (display-content (display-hours hours))))

(defn display-registration
  ([{:keys [hours projects now]} ] (do-display :start hours (start projects now)))
  ([{:keys [hours projects now]} period] (do-display :stop hours (stop period))))
