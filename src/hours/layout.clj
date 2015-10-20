(ns hours.layout
    (:require
      [hiccup.core :refer [html]]
      [hiccup.page :refer [html5 include-js include-css]]
      [ring.util.anti-forgery :refer [anti-forgery-field]]
      [ring.util.response]
      [clj-time.core :as t]
      [clj-time.format :as f]
      [clj-time.coerce :as c]      
      [hours.time :as time]
      [hours.security :as security]
      ))

(defn basic-date [date]
  (f/unparse (f/formatters :basic-date) date))

(defn display-week-chooser [client-id mon]
  (let [prev-week (basic-date (time/prev-week mon))
        next-week  (basic-date (time/next-week mon))]
    [:ul.pull-left.pagination
     [:li [:a {:href (str  "/report/by-week/" client-id "/" prev-week)} "<"]]
     (if (= mon (time/prev-monday (t/now)))
       [:li [:span {:style "color: #777"} "This week"] ]
       (list  [:li [:span  {:style "color: #777"} (str (f/unparse time/display-date-formatter mon) " - " (f/unparse time/display-date-formatter (t/plus mon (t/days 6))))]]
              [:li [:a {:href (str  "/report/by-week/" client-id "/" next-week)} ">"]]))]))

(defn display-project [project client]
  [:td  [:h5 {:style "margin-top: 0px; margin-bottom: 0px"} project "&nbsp;" [:small client]]])

(defn sum [acc period]
  (let [start (c/from-sql-time (:period_start period))
        stop (c/from-sql-time (:period_end period))
        minutes (t/in-minutes (t/interval start stop))]
    (+ acc minutes)))

(defn display-project-day [[key periods]]
  [:tr
   [:td (time/format-with-tz (:period-start key) time/display-date-formatter)]
   (display-project (get-in key [:project :name]) (get-in key [:client :name]))
   [:td.text-right (time/format-minutes (reduce sum 0 periods))]])

(defn find-distinct-clients [report]
  (->> report
       (map (fn [r] {:id (get-in (first r) [:client :id]) :name (get-in (first r) [:client :name])} ))
       (distinct)
       (into [])))

(defn display-client-li [date client]
  [:li [:a {:href (str "/report/by-week/" (:id client) "/" date) } (:name client)]])

(defn grand-total [report]
  (->> report
       (mapcat (fn [[_ val]] val))
       (reduce sum 0)
       (time/format-minutes)))

(defn display-report [client-id date report]
  (let [date-str (basic-date date)]
    [:div
     [:h1 "Weekly report" [:span.small.pull-right (display-week-chooser client-id date)] ]    
     [:table.table
      [:tbody
       [:tr
        [:th "Date"]
        [:th.dropdown  [:a.dropdown-toggle {:href "#" :data-toggle "dropdown" :role "button"
                                            :aria-haspopup "true" :aria-expanded "false"} "Project" [:span.caret]]
         [:ul.dropdown-menu
          (map (partial display-client-li date-str)  (conj (find-distinct-clients report) {:id ":all" :name "All"}))
          ]]     
        [:th.text-right "Total"]]
       (map display-project-day report)
       [:tr
        [:td "&nbsp;"]
        [:td "&nbsp;"]        
        [:td.text-right (grand-total report)]]]]]))

(defn display-edit-period-end [period]
  [:form {:method "POST" :action (str "/period/" (:id period))}
   [:input {:type "hidden" :name "date" :id "date" :value (time/->date-str (c/from-sql-time (:period_start period)))}]
   [:input {:type "hidden" :name "project" :id "project" :value (:name_2 period)}]
   [:input {:type "hidden" :name "start" :id "start" :value (time/->hh:mm-str (c/from-date (:start period)))}]
   [:div.input-group.col-xs-3
    [:input.form-control {:type "text" :name "end" :size "5" :maxlength "5"}]
    [:span.input-group-btn
     [:button.btn.btn-default {:type "submit" } "Stop"]]]])

(defn display-edit-period [period]
  [:form {:method "POST" :action (str "/period/" (:id period))}
   [:div.form-group
    [:label {:for "date"} "Date"]
    [:input.form-control {:type "text" :name "date" :id "date" :value (time/->date-str (c/from-sql-time (:period_start period)))}]]
   [:div.form-group
    [:label {:for "project"} "Project"]
    [:input.form-control {:type "text" :name "project" :id "project" :value (:name_2 period)}]]
   [:div.form-group
    [:label {:for "start"} "Start"]
    [:input.form-control {:type "text" :name "start" :id "start" :value (time/->hh:mm-str (c/from-date (:period_start period)))}]]
   [:div.form-group
    [:label {:for "end"} "End"]
    [:input.form-control {:type "text" :name "end" :id "end" :value (time/->hh:mm-str (c/from-sql-date (:period_end period)))}]]
   [:button.btn.btn-default {:type "submit"} "Go!"]])

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
         [:td  (time/format-with-tz start time/display-date-formatter)]
         (display-project (:name_2 hour) (:name_3 hour))
                  
         [:td (when start (time/->hh:mm-str start) ) " - "
          (when stop 
            (time/->hh:mm-str stop))]
         
         [:td.text-right (time/format-interval (time/->hour-mins diff))]
         [:td.text-right [:a {:href (str "/period/" (:id hour))} "edit"] " | " [:a {:href (str "/period/" (:id hour) "/delete")} "delete"]]]))]])

(defn start-stop [action period-id project content]
  [:div.row
   [:form {:method "POST" :action (str "/user/register/" action) }
    [:div.input-group
     (if (= action "start")
       (list
        [:div.input-group-btn
         [:input.form-control {:type "text" :style "width: 5em" :name "date" :id "date-container" :value (time/->date-dd.mm (t/now))}]]
        [:input.form-control {:type "text" :name "project" :placeholder "Project name..."}]
        [:script "$('#date-container').datepicker({ format: 'dd/mm', weekStart: 1, calendarWeeks: true, autoclose: true, todayHighlight: true, endDate: 'today', orientation: 'top left'});" ])
       (list
        [:input {:type "hidden" :name "period-id" :value (str period-id)}]
        [:input.form-control {:type "text" :name "project" :value project :readonly "readonly"}]))
     (if (= action "start")
       [:span.input-group-btn [:button.btn.btn-success {:type "submit" :value "start"} "start"]]
       [:span.input-group-btn [:button.btn.btn-default {:type "submit" :value "stop"} "stop"]])]] content])

(defn include-styling []
  (list [:script {:src "https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"}]
        (include-css  "/css/bootstrap.min.css" "/css/bootstrap-social.css" "/css/font-awesome.min.css"
                      "/css/bootstrap-datepicker3.min.css" "/css/hours.css")
        (include-js "/js/bootstrap.min.js" "/js/bootstrap-datepicker.min.js")))

(defn display-user-nav-bar [userinfo]
  [:li [:p.navbar-text (get userinfo "name") "&nbsp;" [:img {:src (get userinfo "picture") :width "20"}]]])

(defn render-footer []
  [:footer.footer {:role "contentinfo"}
     [:div.container
      [:hr
       [:p [:i.fa.fa-github] "&nbsp;"[:a {:href "https://github.com/slipset/"} "slipset"] "/"
        [:a {:href "https://github.com/slipset/hours/"} "hours"] " | " [:i.fa.fa-twitter] [:a {:href "https://twitter.com/slipset/"} "slipset"]] ]]])

(defn render-navbar
  ([] (render-navbar nil))
  ([logged-in-user]
   [:nav.navbar.navbar-inverse {:role "banner"}
    [:div.container-fluid
     [:div.navbar-header
      (when logged-in-user
        [:button.navbar-toggle {:type "button" :data-toggle "collapse" :data-target "#myNavbar"}
         [:span.icon-bar]
         [:span.icon-bar]
         [:span.icon-bar]])
      [:a.navbar-brand {:href "/"} "workday"]]
     (when logged-in-user
       [:div.collapse.navbar-collapse {:id "myNavbar"}
        [:ul.nav.navbar-nav
         [:li [:a {:href "/user"} "Home"]]
         [:li [:a {:href "/user/status"} "Status"]]
         [:li [:a {:href "/client"} "Clients"]]
         [:li.dropdown
          [:a.dropdown-toggle {:href "#" :data-toggle "dropdown" :role "button"
                               :aria-haspopup "true" :aria-expanded "false"} "Reports" [:span.caret]]
          
          [:ul.dropdown-menu
           [:li [:a {:href "/report/by-week"} "Weekly"]]]]]
        [:ul.nav.navbar-nav.navbar-right
         (display-user-nav-bar logged-in-user)
         [:li [:a {:href "/logout"} [:span.glyphicon.glyphicon-log-out] "Logout"]]]
        ])]]))

(defn render-login []
  (list  [:div.page-header
             [:h1 "Sign in"]]
            [:div.row
             [:div.col-lg-4.col-lg-offset-4.col-md-4.col-md-offset-4.col-sm-6.col-sm-offset-3
              [:a.btn.btn-block.btn-social.btn-google {:href  "/user/"}
               [:i.fa.fa-google] "Sign in with Google" ]]]))

(defn display-clients [clients]
  (list [:table.table
         [:tbody
          [:tr
           [:th "Name"]
           [:th "&nbsp;"]]
          (for [client clients]
            [:tr
             [:td (:name client)]
             [:td [:a {:href (str "/project/add/" (:id client))} "Add project"] " | " [:a {:href (str "/client/" (:id client) "/projects")} "Show projects"]]])]]
        [:div [:a {:href "/client/add"} "Add client"]]))

(defn display-add-client []
  [:form {:method "POST" :action "/client/add"}
   (anti-forgery-field)
   [:div.input-group
    [:input.form-control {:type "text" :name "name" :placeholder "Client name..."}]
     [:span.input-group-btn
      [:button.btn.btn-default {:type "submit"} "Add"]]]])

(defn display-projects [projects]
  (let [client-id (:workday_client_id (first projects))]
    [:div  [:table.table
            [:tbody
             [:tr
              [:th "Name"]
              [:th "Client" ]]
             (for [project projects]
               [:tr
                [:td (:name project)]
                [:td (:name_2 project)]])]]
     [:div [:a {:href (str "/project/add/" client-id)} "Add project"]]]))

(defn display-add-project [client]
  [:form {:method "POST" :action (str "/project/add/" (:id client))}
    (anti-forgery-field)
   [:div.input-group
    [:input.form-control {:type "text" :name "name" :placeholder "project name..."}]
     [:span.input-group-btn
      [:button.btn.btn-default {:type "submit"} "Add"]]]])

(defn display-not-found [request]
  (list [:h1 (str "404 Not found: " (:uri request))]
   [:iframe {:width "560" :height "315" :src "https://www.youtube.com/embed/O_ISAntOom0" :frameborder "0" } ]))

(defn wrap-page-template [f]
  (fn [page] (f (list
                 [:head
                  [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0, user-scalable=no"}]
                  [:title "hours"]
                  (include-styling)]
                 [:body
                  (:navbar page) 
                  [:div.container (:content page)]
                  (:footer page)]))))

(defn wrap-navbar [f user]
  (fn [page]
    (f (assoc page :navbar (render-navbar user)))))

(defn wrap-footer [f]
  (fn [page]
    (f (assoc page :footer (footer)))))

(defn page-renderer
  ([] (fn [content] (page-template  content (footer))))
  ([user] (fn [content] (page-template (render-navbar user) content (footer)))))

(defn render-html5 [page]
   (html5 page))

(defn wrap-in-content [f]
  (fn [page]
    (f {:content page})))

(defn get-html5
  ([] (create-page-renderer render-html5 nil))
  ([user] (create-page-renderer render-html5 user)))

(defn create-page-renderer [renderer user]
  (-> renderer
      (wrap-page-template)
      (wrap-navbar user)
      (wrap-footer)
      (wrap-in-content)))

(defn display-status-page [request]
  (let [count (:count (:session request) 0)
        session (assoc (:session request) :count (inc count))]
    (-> (ring.util.response/response
              (str "<p>We've hit the session page " (:count session)
                   " times.</p><p>The current session: " session "</p>"))
        (assoc :session session))))
