(ns hours.layout
    (:require
      [hiccup.core :refer [html ]]
      [hiccup.form :refer [select-options ]]      
      [hiccup.page :refer [html5 include-js include-css]]
      [ring.util.anti-forgery :refer [anti-forgery-field]]
      [ring.util.response]
      [clj-time.core :as t]
      [clj-time.format :as f]
      [clj-time.coerce :as c]      
      [hours.time :as time]
      [hours.security :as security]
      ))

(defn display-project [project client color]
  [:h5 {:style "margin-top: 0px; margin-bottom: 0px;"} [:span {:style (str "padding:2px;background-color:#" color)} project] "&nbsp;" [:small client]])

(defn display-edit-period [period projects]
  [:form {:method "POST" :action (str "/period/" (:id period))}
   [:div.form-group
    [:label {:for "date"} "Date"]
    [:input.form-control {:type "text" :name "date"
                          :id "date" :value (time/->date-str (c/from-sql-time (:period_start period)))}]]
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
    [:input.form-control {:type "text" :name "start" :id "start" :value (time/->hh:mm-str (c/from-date (:period_start period)))}]]
   [:div.form-group
    [:label {:for "end"} "End"]
    [:input.form-control {:type "text" :name "end" :id "end" :value (when-let [end (:period_end period)] (time/->hh:mm-str (c/from-sql-date end)))}]]
   [:button.btn.btn-default {:type "submit"} "Go!"]])





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
              [:th "Client" ]
              [:th "&nbsp;"]]
             (for [project projects]
               [:tr
                [:td [:span {:style (str "background-color:#" (:color project))} (:name project)]]
                [:td (:name_2 project)]
                [:td [:a {:href (str "/project/edit/" (:id project))} "edit"]]])]]
     [:div [:a {:href (str "/project/add/" client-id)} "Add project"]]]))

(def colors ["D4D8D1" "A8A8A1" "AA9A66" "B74934" "577492" "67655D" "332C2F"])

(defn display-color-li [color]
  [:li {:data-value color} [:a {:href "#"} [:span {:style (str "display:inline-block;width:20px;height:15px;background-color:#" color)}] [:span.value (str "#" color) ]]])

(defn display-color-chooser [colors]
  (list
   [:button.form-control.btn.btn-default.dropdown-toggle {:type "button" :data-toggle "dropdown" :id "color-label" }
    [:span.value {} "Color"] [:span.caret]]
   [:input {:type "hidden" :id "color" :name "color"}]
   [:ul.dropdown-menu
    (map display-color-li colors)]
   [:script "$('.dropdown-menu li').click(function () {
    var color = $(this).attr('data-value');
    $('#color-label').attr('style', 'background-color:#' + color)
    $('#color').val(color);
    console.log( $('#color').val());});"]))

(defn display-add-project [client]
  [:form {:method "POST" :action (str "/project/add/" (:id client))}
   (anti-forgery-field)

   [:div.input-group-btn {:style "width:70%"}
    [:input.form-control {:type "text" :name "name" :placeholder "project name..."}]]
   [:div.input-group-btn {:style "width:10%"}
    (display-color-chooser colors)]
    [:span.input-group-btn
     [:button.btn.btn-default {:type "submit"} "Add"]]])

(defn display-add-project [client]
  [:form {:method "POST" :action (str "/project/add/" (:id client))}
   (anti-forgery-field)

   [:div.input-group-btn {:style "width:70%"}
    [:input.form-control {:type "text" :name "name" :placeholder "project name..."}]]
   [:div.input-group-btn {:style "width:10%"}
    (display-color-chooser colors)]
    [:span.input-group-btn
     [:button.btn.btn-default {:type "submit"} "Add"]]])

(defn display-edit-project [project]
  [:form {:method "POST" :action (str "/project/edit/" (:id project))}
   (anti-forgery-field)
   [:div.input-group-btn {:style "width:70%"}
    [:input.form-control {:type "text" :name "name" :value (:name project)} ]]
   [:div.input-group-btn {:style "width:10%"}
    (display-color-chooser colors)]
    [:span.input-group-btn
     [:button.btn.btn-success {:type "submit"} "Go!"]]])

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
    (f (assoc page :footer (render-footer)))))

(defn wrap-in-content [f]
  (fn [page]
    (f {:content page})))

(defn render-html5 [page]
   (html5 page))

(defn create-page-renderer [renderer user]
  (-> renderer
      (wrap-page-template)
      (wrap-navbar user)
      (wrap-footer)
      (wrap-in-content)))

(defn get-html5
  ([] (create-page-renderer render-html5 nil))
  ([user] (create-page-renderer render-html5 user)))

(defn display-status-page [request]
  (let [count (:count (:session request) 0)
        session (assoc (:session request) :count (inc count))]
    (-> (ring.util.response/response
              (str "<p>We've hit the session page " (:count session)
                   " times.</p>"
                   "<p>The current session: " session "</p>"
                   "<p>The curren request:" request "</p>"))
        (ring.util.response/header "Content-type" "text/html; charset=utf-8")
        (assoc :session session))))
