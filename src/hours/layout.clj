(ns hours.layout
    (:require
      [hiccup.core :refer [html]]
      [hiccup.page :refer [html5 include-js include-css]]
      [ring.util.anti-forgery :refer [anti-forgery-field]]
      [ring.util.response]
      [clj-time.core :as t]
      [clj-time.format :as f]
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

(defn display-hours [hours user-id]
  [:table.table
   [:tbody
    [:tr
     [:th "Date"]
     [:th "Project"]     
     [:th "Start"]
     [:th "End"]
     [:th "Total"]]
    (for [hour (time/by-date (user-id hours))]
      (let [start (:start (second hour) )
            stop (:stop (second hour) )
            diff (t/interval start stop)]
        [:tr
         [:td  (first (first hour))]
         [:td  (second (first hour))]         
          [:td (f/unparse (f/formatters :hour-minute) start) ]
          [:td (f/unparse (f/formatters :hour-minute) stop) ]
          [:td (time/format-interval (time/->hour-mins diff))]]))]])

(defn start-stop [action project content]
  [:div
   [:form {:method "POST" :action (str "/user/register/" action) }
    (anti-forgery-field)
    [:div.input-group
     (if (= action "start")
       [:input.form-control {:type "text" :name "project" :placeholder "Project name..."}]
       [:input.form-control {:type "text" :name "project" :value project :readonly "readonly"}])
     [:span.input-group-btn
      [:button.btn.btn-default {:type "submit" :value action} action]]
     ]]
   content])

(defn include-styling []
  (list [:script {:src "https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"}]
  (include-css  "/css/bootstrap.min.css" "/css/bootstrap-social.css" "/css/font-awesome.min.css")
  (include-js "/js/bootstrap.min.js" )))

(defn display-user-nav-bar [userinfo]
  [:li [:p.navbar-text (get userinfo "name") "&nbsp;" [:img {:src (get userinfo "picture") :width "20"}]]])

(defn page-template [logged-in-user content]
  (html5
   [:head
   ; [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0, user-scalable=no"}]
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
        [:li [:a {:href "/client"} "Clients"]]]
       [:ul.nav.navbar-nav.navbar-right
        (display-user-nav-bar logged-in-user)
        [:li [:a {:href "/logout"} [:span.glyphicon.glyphicon-log-out] "Logout"]]]
       ]]]
    [:div.container content]
    [:div.row
     ]]))

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
  [:table.table
   [:tbody
    [:tr
     [:th "Name"]]
    (for [client clients]
      [:tr
       [:td (:name client)]])]])

(defn show-week-page [logged-in-user date]
  (page-template logged-in-user (display-week (time/week (f/parse (f/formatters :basic-date) date)))))

(defn show-hours-page [logged-in-user action content hours]
  (page-template logged-in-user (start-stop action content (display-hours hours (security/user-id-kw)))))

(defn show-clients-page [logged-in-user clients]
  (page-template logged-in-user (display-clients clients)))
