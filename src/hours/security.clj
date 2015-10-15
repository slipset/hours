(ns hours.security
    (:require
      [friend-oauth2.workflow :as oauth2]
      [friend-oauth2.util     :refer [format-config-uri]]
      [environ.core           :refer [env]]
      [clj-http.client :as client]
      [hours.user :as user]
      [cheshire.core :as parse]))

(def current-user (atom {}))

(def user ::user)

(def client-config
  {:client-id     (env :hours-oauth2-client-id)
   :client-secret (env :hours-oauth2-client-secret)
   :callback      {:domain (env :hours-uri) 
                   :path "/oauth2callback"}})

(defn google-user-details [token]
  (->> (str "https://www.googleapis.com/oauth2/v1/userinfo?access_token=" token)
       (client/get)
       :body
        (parse/parse-string)))

(defn get-or-create-user-id [db-spec user-info]
  (let [email (get user-info "email")
        user-id (user/user-by-email {:email email} {:connection db-spec})]
    (if (seq user-id) 
      (first user-id) 
      (first  (user/add-user<! {:first_name (get user-info "given_name")
                                :last_name (get user-info "family_name")
                                :email email} {:connection db-spec})))))

(defn credential-fn [db-spec token]
  (let [user-info (google-user-details (:access-token token))
        user-id (:id (get-or-create-user-id db-spec user-info))]

    (reset! current-user (assoc user-info :workday-id user-id))
    {:identity token
     :user-info @current-user
     :roles #{::user}}))

(def uri-config
  {:authentication-uri {:url "https://accounts.google.com/o/oauth2/auth"
                        :query {:client_id (:client-id client-config)
                               :response_type "code"
                               :redirect_uri (format-config-uri client-config)
                               :scope "email"}}

   :access-token-uri {:url "https://accounts.google.com/o/oauth2/token"
                      :query {:client_id (:client-id client-config)
                              :client_secret (:client-secret client-config)
                              :grant_type "authorization_code"
                              :redirect_uri (format-config-uri client-config)}}})

(defn friend-config [db-spec]
  {:allow-anon? true
   :workflows   [(oauth2/workflow
                  {:client-config client-config
                   :uri-config uri-config
                   :credential-fn (partial credential-fn db-spec)})
                   ]})

(defn logout []
  (reset! current-user {}))

(defn logged-in-user []
  @current-user)

(defn user-id []
  (->@current-user
      :workday-id
       str))

(def user-id-kw (comp keyword user-id))
