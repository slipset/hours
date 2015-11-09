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

(defn display-client-li [date client]
  [:li [:a {:href (str "/report/by-week/" (:id client) "/" date) } (:name client)]])

(defn display-day-total [report dt]
  [:td  (->> report 
             (keep (fn [[k v]] (when (= (:period-start k) dt) v)))
             (flatten)
             (reduce sum 0)
             (time/format-minutes))])

(defn display-week-chooser [client-id start end]
  (let [prev-week (time/basic-date (time/prev-week start))
        next-week  (time/basic-date (time/next-week end))]
    [:ul.pull-left.pagination
     [:li [:a {:href (str  "/report/by-week/" client-id "/" prev-week)} "<"]]
     (if (= start (time/prev-monday (t/now)))
       [:li [:span {:style "color: #777"} "This week"] ]
       (list  [:li [:span  {:style "color: #777"} (str (f/unparse time/display-date-formatter start) " - " (f/unparse time/display-date-formatter end))]]
              [:li [:a {:href (str  "/report/by-week/" client-id "/" next-week)} ">"]]))]))

(defn display-project-day [report project dt]
  (let [client (:client project)
        key {:period-start dt :project (dissoc project :client :color) :client client}
        periods (get report key)]
    [:td  (time/format-minutes (reduce sum 0 periods))]))

(defn display-project-week [report period-start project]
  [:tr
   [:td (display-project (:name project) (get-in project [:client :name]) (:color project))]
   (map (partial display-project-day report project) (time/week period-start))])

(defn display-weekly-report [{:keys [client-id report grand-total date clients projects period-start period-end]}]
  (let [date-str (time/basic-date period-start)]
    [:div
     [:h1 "Weekly report" [:span.small.pull-right (display-week-chooser client-id period-start period-end)] ]    
     [:table.table
      [:tbody
       [:tr
        [:th.dropdown  [:a.dropdown-toggle {:href "#" :data-toggle "dropdown" :role "button"
                                            :aria-haspopup "true" :aria-expanded "false"} "Project" [:span.caret]]
         [:ul.dropdown-menu
          (map (partial display-client-li date-str) clients)]]
        (map (fn [dt] [:th (time/basic-day dt)]) (time/week period-start))
        [:th.text-right "Total"]]
       (map (partial display-project-week report period-start) projects)
       [:tr
        [:td "&nbsp;"]
        (map (partial display-day-total report) (time/week period-start))
        [:td.text-right (time/format-minutes grand-total)]]]]]))
