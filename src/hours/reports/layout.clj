(ns hours.reports.layout
    (:require
      [clj-time.core :as t]
      [clj-time.format :as f]
      [clj-time.coerce :as c]
      [hours.time :as time]))

(defn display-project [project client color]
  [:h5 {:style "margin-top: 0px; margin-bottom: 0px;"}
   [:span {:style (str "padding:2px;background-color:#" color)} project] "&nbsp;" [:small client]])

(defn week-url [client-id start]
  (str  "/report/by-week/" client-id "/" start))

(defn month-url [client-id start]
  (str  "/report/by-month/" client-id "/" start))

(defn display-client-li [date client]
  [:li [:a {:href (week-url (:id client) date) } (:name client)]])

(defn display-day-total [day-total]
  [:td (time/format-minutes day-total)])

(defn display-week-chooser [client-id period]
  (let [start (first period)
        end (last period)
        prev-week (time/basic-date (time/prev-week start))
        next-week  (time/basic-date (time/next-week end))
        prev-url (week-url client-id prev-week)]
    [:ul.pull-left.pagination
     [:li [:a {:href prev-url} "<"]]
     (if (= start (time/prev-monday (t/now)))
       [:li [:span {:style "color: #777"} "This week"] ]
       (list  [:li [:span  {:style "color: #777"} (str (f/unparse time/display-date-formatter start)
                                                       " - " (f/unparse time/display-date-formatter end))]]
              [:li [:a {:href (week-url client-id next-week)} ">"]]))]))

(defn display-month-chooser [client-id period]
  (let [start (first period)
        end (last period)
        prev (time/basic-date (time/prev-week start))
        next  (time/basic-date (time/next-week end))
        prev-url (month-url client-id prev)]
    [:ul.pull-left.pagination
     [:li [:a {:href prev-url} "<"]]
     (if (= start (time/prev-month (t/now)))
       [:li [:span {:style "color: #777"} "This month"] ]
       (list  [:li [:span  {:style "color: #777"} (str (f/unparse time/display-date-formatter start)
                                                       " - " (f/unparse time/display-date-formatter end))]]
              [:li [:a {:href (month-url client-id next)} ">"]]))]))

(defn display-project-week [[project days]]
    [:tr
     [:td (display-project (get-in project [:project :name])
                           (get-in project [:client :name])
                           (get-in project [:project :color]))]
     (map (fn [d] [:td (time/format-minutes (:total d))])  days)])

(defn display-weekly-report [{:keys [client-id report day-totals date clients projects period]}]
  (let [date-str (time/basic-date (first period))
        week-chooser (display-week-chooser client-id period)
        client-dropdown (map (partial display-client-li date-str) clients)
        week-days (map (fn [dt] [:th (time/basic-day dt)]) period)
        project-week (map display-project-week report)
        day-totals (map display-day-total day-totals)]
   [:div.row
     [:h1 "Weekly report" [:span.small.pull-right week-chooser] ]    
     [:table.table
      [:tbody
       [:tr
        [:th.dropdown  [:a.dropdown-toggle {:href "#" :data-toggle "dropdown" :role "button"
                                            :aria-haspopup "true" :aria-expanded "false"} "Project" [:span.caret]]
         [:ul.dropdown-menu
          client-dropdown]]
        week-days
        [:th.text-right "Total"]]
       project-week
         [:tr
          [:td "&nbsp;"]
          day-totals]]]]))

(defn display-monthly-report [{:keys [client-id report day-totals date clients projects period]}]
  (let [date-str (time/basic-date (first period))
        month-chooser (display-month-chooser client-id period)
        client-dropdown (map (partial display-client-li date-str) clients)
        week-days (map (fn [dt] [:th (time/basic-day dt)]) period)
        project-week (map display-project-week report)
        day-totals (map display-day-total day-totals)]
   [:div.row
     [:h1 "Monthly report" [:span.small.pull-right month-chooser] ]    
     [:table.table
      [:tbody
       [:tr
        [:th.dropdown  [:a.dropdown-toggle {:href "#" :data-toggle "dropdown" :role "button"
                                            :aria-haspopup "true" :aria-expanded "false"} "Project" [:span.caret]]
         [:ul.dropdown-menu
          client-dropdown]]
        week-days
        [:th.text-right "Total"]]
       project-week
         [:tr
          [:td "&nbsp;"]
          day-totals]]]]))
