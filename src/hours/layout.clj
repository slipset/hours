(ns hours.layout
    (:require
      [hiccup.form :refer [select-options ]]      
      [hiccup.page :refer [html5 include-js include-css]]
      [ring.util.anti-forgery :refer [anti-forgery-field]]
      [ring.util.response]
      [clj-time.core :as t]
      ))

(defn include-styling []
  (list [:script {:src "https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"}]
        (include-css  "/css/bootstrap.min.css" "/css/bootstrap-social.css" "/css/font-awesome.min.css"
                      "/css/bootstrap-datepicker3.min.css" "/css/hours.css")
        (include-js "/js/bootstrap.min.js" "/js/bootstrap-datepicker.min.js")))

(defn display-user-nav-bar [userinfo]
  [:li [:p.navbar-text (get userinfo "name") "&nbsp;" [:img {:src (get userinfo "picture") :width "20"}]]])

(defmacro insert-sha []
  (let [sha (slurp "../../.git/refs/heads/master")]
    (.substring sha 0 6)))

(defn render-footer []
  [:footer.footer {:role "contentinfo"}
     [:div.container
      [:hr
       [:p [:i.fa.fa-github] "&nbsp;"[:a {:href "https://github.com/slipset/"} "slipset"] "/"
        [:a {:href "https://github.com/slipset/hours/"} "hours"] " | " [:i.fa.fa-twitter] [:a {:href "https://twitter.com/slipset/"} "slipset"] " | sha:" (insert-sha)] ]]])

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
