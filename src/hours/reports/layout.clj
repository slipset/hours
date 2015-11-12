(ns hours.reports.layout
    (:require
      [clj-time.core :as t]
      [clj-time.format :as f]
      [clj-time.coerce :as c]
      [hours.time :as time]))

(defn display-project [project client color]
  [:h5 {:style "margin-top: 0px; margin-bottom: 0px;"}
   [:span {:style (str "padding:2px;background-color:#" color)} project] "&nbsp;" [:small client]])

(defn sum [acc period]
  (let [start (c/from-sql-time (:period_start period))
        stop (c/from-sql-time (:period_end period))
        minutes (t/in-minutes (t/interval start stop))]
    (+ acc minutes)))

(defn week-url [client-id week]
  (str  "/report/by-week/" client-id "/" week))

(defn display-client-li [date client]
  [:li [:a {:href (week-url (:id client) date) } (:name client)]])

(defn display-day-total [day-total]
  [:td  (time/format-minutes day-total)])

(defn display-week-chooser [client-id start end]
  (let [prev-week (time/basic-date (time/prev-week start))
        next-week  (time/basic-date (time/next-week end))
        prev-url (week-url client-id prev-week)]
    [:ul.pull-left.pagination
     [:li [:a {:href prev-url} "<"]]
     (if (= start (time/prev-monday (t/now)))
       [:li [:span {:style "color: #777"} "This week"] ]
       (list  [:li [:span  {:style "color: #777"} (str (f/unparse time/display-date-formatter start) " - " (f/unparse time/display-date-formatter end))]]
              [:li [:a {:href (week-url client-id next-week)} ">"]]))]))


(defn display-project-week [[project days]]
    [:tr
     [:td (display-project (get-in project [:project :name])
                           (get-in project [:client :name])
                           (get-in project [:project :color]))]
     (map (fn [d] [:td  (time/format-minutes (:total d))])  days)])

(defn display-weekly-report [{:keys [client-id report day-totals grand-total date clients projects period-start period-end]}]
  (let [date-str (time/basic-date period-start)
        week (time/week period-start)
        week-chooser (display-week-chooser client-id period-start period-end)
        client-dropdown (map (partial display-client-li date-str) clients)
        week-days (map (fn [dt] [:th (time/basic-day dt)]) week)
        project-week (map display-project-week report)
        day-totals (map display-day-total day-totals)
        total-hours (time/format-minutes grand-total)]
   [:div
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
          day-totals
          [:td.text-right total-hours]]]]]))
