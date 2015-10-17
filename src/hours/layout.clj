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

(defn display-week-chooser [mon]
  (let [prev (time/prev-week mon)
        next (time/next-week mon)]
    [:div "Uke " [:a {:href (str "/user/week/" (f/unparse (f/formatters :basic-date) prev )) } (.getWeekOfWeekyear prev) ]
     " "
     (.getWeekOfWeekyear mon)
     " "
     [:a {:href (str "/user/week/" (f/unparse (f/formatters :basic-date) next )) } (.getWeekOfWeekyear next) ]
     ]))

(defn display-week [week]
  [:div
   (display-week-chooser (first week))
   [:table {:border "1"}
   [:tbody
    [:tr
     [:th "Date"] [:th "Project"][:th "From"] [:th "To"] [:th "Sum"] [:th "Extra"] [:th "Iterate"] [:th "Total"] [:th "&nbsp;"]]
    (for [day week]
      [:form {:method "POST" :action (str "/user/register/" (f/unparse (f/formatters :basic-date) day))}
       (anti-forgery-field)
       [:tr
        [:td (f/unparse time/custom-formatter day)]
        [:td [:input {:type "text" :name "from"}]]
        [:td [:input {:type "text" :name "to"}]]
        [:td "&nbsp;"]
        [:td [:input {:type "text" :name "extra"}]]
        [:td [:input {:type "text" :name "iterate"}]]
        [:td "&nbsp;"]
        [:td [:input {:type "submit" :value "Go"}]]]])]]])

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
   [:button.btn.btn-default {:type "submit"} "Go!"] 
   ])

(defn display-hours [hours user-id]
  [:table.table
   [:tbody
    [:tr
     [:th "Date"]
     [:th "Project"]     
     [:th "Start"]
     [:th "End"]
     [:th "Total"]
     [:th "&nbsp;"]]
    (for [hour hours]
      (let [start (c/from-sql-time (:period_start hour)) 
            stop (c/from-sql-time (:period_end hour))
            diff (t/interval start stop)]
        [:tr
         [:td  (f/unparse time/custom-formatter start)]
         [:td  (str (:name_3 hour) "/" (:name_2 hour) ) ]         
         [:td (when start (f/unparse (f/formatters :hour-minute) start))  ]
         [:td (when stop
                (f/unparse (f/formatters :hour-minute) stop)) ]
         [:td (time/format-interval (time/->hour-mins diff))]
         [:td [:a {:href (str "/period/" (:id hour))} "edit"] " | " [:a {:href (str "/period/" (:id hour) "/delete")} "delete"]]]))]])

(defn start-stop [action period-id project content]
  [:div.row
   [:form.form-inline {:method "POST" :action (str "/user/register/" action) }
    (anti-forgery-field)
    [:div {}
     (if (= action "start")
       (list
        [:input.form-control {:type "text" :name "date" :id "date-container"   :value (time/->date-dd.mm (t/now))}]
        [:input.form-control {:type "text" :name "project" :placeholder "Project name..."}]
        [:script "$('#date-container').datepicker({ format: 'dd/mm', weekStart: 1, calendarWeeks: true, autoclose: true, todayHighlight: true, endDate: 'today', orientation: 'top left'});" ])
       (list
        [:input {:type "hidden" :name "period-id" :value (str period-id)}]
        [:input.form-control {:type "text" :name "project" :value project :readonly "readonly"}]))
     [:button.btn.btn-default {:type "submit" :value action} action]]] content])

(defn include-styling []
  (list [:script {:src "https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"}]
        (include-css  "/css/bootstrap.min.css" "/css/bootstrap-social.css" "/css/font-awesome.min.css"
                      "/css/bootstrap-datepicker3.min.css")
        (include-js "/js/bootstrap.min.js" "/js/bootstrap-datepicker.min.js")))

(defn display-user-nav-bar [userinfo]
  [:li [:p.navbar-text (get userinfo "name") "&nbsp;" [:img {:src (get userinfo "picture") :width "20"}]]])

(defn page-template [logged-in-user content]
  (html5
   [:head
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0, user-scalable=no"}]
    [:title "hours"]
    (include-styling)]
   [:body
    [:nav.navbar.navbar-inverse
     [:div.container-fluid
      [:div.navbar-header
       [:button.navbar-toggle {:type "button" :data-toggle "collapse" :data-target "#myNavbar"}
        [:span.icon-bar]
        [:span.icon-bar]
        [:span.icon-bar]]
       [:a.navbar-brand {:href "/user"} "workday"]]
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
       ]]]
    [:div.container content]]))

(defn login-page []
  (html5
   [:head
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0, user-scalable=no"}]
    [:title "hours"]
    (include-styling)]
   [:body
    [:nav.navbar.navbar-inverse {:role "banner"}
     [:div.container-fluid
      [:div.navbar-header
       [:a.navbar-brand {:href "/"} "workday"]]]]

    [:div.container
     [:div.page-header
      [:h1 "Sign in"]]
     [:div.row
      [:div.col-lg-4.col-lg-offset-4.col-md-4.col-md-offset-4.col-sm-6.col-sm-offset-3
       [:a.btn.btn-block.btn-social.btn-google {:href  "/user/"}
        [:i.fa.fa-google] "Sign in with Google" ]]]]]))

(defn show-status-page [logged-in-user request]
  (let [count (:count (:session request) 0)
        session (assoc (:session request) :count (inc count))]
    (-> (ring.util.response/response
              (page-template logged-in-user
               (str "<p>We've hit the session page " (:count session)
                    " times.</p><p>The current session: " session "</p>")))
        (assoc :session session))))

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
  [:iframe {:width "560" :height "315" :src "https://www.youtube.com/embed/O_ISAntOom0" :frameborder "0" } ])

(defn show-week-page [logged-in-user date]
  (page-template logged-in-user (display-week (time/week (f/parse (f/formatters :basic-date) date)))))

(defn show-hours-page [logged-in-user action content period-id periods]
  (page-template logged-in-user (start-stop action period-id content (display-hours periods nil))))

(defn show-clients-page [logged-in-user clients]
  (page-template logged-in-user (display-clients clients)))

(defn show-add-client-page [logged-in-user]
  (page-template logged-in-user (display-add-client)))

(defn show-projects-page [logged-in-user projects]
  (page-template logged-in-user (display-projects projects)))

(defn show-add-project-page [logged-in-user client]
  (page-template logged-in-user (display-add-project client)))

(defn show-edit-period-page [logged-in-user period]
  (page-template logged-in-user (display-edit-period period)))

(defn show-not-found [logged-in-user request]
  (page-template logged-in-user (display-not-found request)))
