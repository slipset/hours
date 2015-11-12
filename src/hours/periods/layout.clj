(ns hours.periods.layout
    (:require
      [hours.time :as time]
      [clj-time.coerce :as c]
      [clj-time.time :as t]))

(defn display-edit-period [period projects]
  [:form {:method "POST" :action (str "/period/" (:id period))}
   [:div.form-group
    [:label {:for "date"} "Date"]
    [:input.form-control {:type "date" :name "date"
                          :id "date" :max (time/->date-str (t/now)) :value (time/->date-str (c/from-sql-time (:period_start period)))}]]
   [:div.form-group
    [:label {:for "description"} "Description"]
    [:input.form-control {:type "text" :name "description" :id "description" :value (:description period)}]]

   [:div.form-group
    [:label {:for "project"} "Project"]
    [:select.form-control {:name "project-id" :id "project"}
     (map (fn [p] [:option {:value (:id p) :selected (= (:name p) (:name period))} (str (:name p) " - " (:name_2 p))]) projects)
     ]]
   
   [:div.form-group
    [:label {:for "start"} "Start"]
    [:input.form-control {:type "time" :name "start" :id "start" :value (time/->hh:mm-str (c/from-date (:period_start period)))}]]
   [:div.form-group
    [:label {:for "end"} "End"]
    [:input.form-control {:type "time" :name "end" :id "end" :value (when-let [end (:period_end period)] (time/->hh:mm-str (c/from-sql-date end)))}]]
   [:button.btn.btn-default {:type "submit"} "Go!"]])

