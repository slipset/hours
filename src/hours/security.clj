(ns hours.security
    (:require
      [friend-oauth2.workflow :as oauth2]
      [friend-oauth2.util     :refer [format-config-uri]]
      [environ.core           :refer [env]]
      [clj-http.client :as client]
      [hours.user :as user]
      [cheshire.core :as parse]))

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
        user-id (user/user-by-email {:email email} db-spec)]
    (if (seq user-id) 
      (first user-id) 
      (user/add-user<! {:first_name (get user-info "given_name")
                        :last_name (get user-info "family_name")
                        :email email} db-spec))))

(defn create-identity [db-spec token user-info]
  (let [user-id (:id (get-or-create-user-id db-spec user-info))]
    {:identity token
     :user-info (assoc user-info :workday-id user-id :tz 2)
     :roles #{::user}}))

(defn credential-fn [db-spec token]
  (create-identity db-spec token (google-user-details (:access-token token))))

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

(defn user-info [r]
  (-> r
      :session
      :cemerick.friend/identity
      :authentications
      (vals)
      (first)
      :user-info))

(def user-id (partial (comp str :workday-id user-info)))

(def session {:count 2, :state "t6DtwnG5OyO5OGa-dkV1ng4xyiSo2o-IUExhusFbxDOZfkDCgEO-tTMMhBHmwi-MKHBfzYA6K3RkGElT", :cemerick.friend/identity {:authentications {{:access-token "ya29.DQJgnK1tjzDAMAmHwetcSG-QKExIVgBQaFGiCxgeoFgm4-qpN4ZF4YpUPPHEgT53dwGsgQ"} {:identity {:access-token "ya29.DQJgnK1tjzDAMAmHwetcSG-QKExIVgBQaFGiCxgeoFgm4-qpN4ZF4YpUPPHEgT53dwGsgQ"}, :user-info {"verified_email" true, "hd" "assum.net", "id" "111987899333454790239", "gender" "male", "email" "erik@assum.net", "given_name" "Erik", "name" "Erik Assum", :workday-id #uuid "4d8ec934-3a91-4e0d-9a43-84d901394a48", "link" "https://plus.google.com/111987899333454790239", "picture" "https://lh4.googleusercontent.com/-CDgtp5b87YA/AAAAAAAAAAI/AAAAAAAAABY/6-CDD7NeXEM/photo.jpg", "family_name" "Assum"}, :roles #{:hours.security/user}}}, :current {:access-token "ya29.DQJgnK1tjzDAMAmHwetcSG-QKExIVgBQaFGiCxgeoFgm4-qpN4ZF4YpUPPHEgT53dwGsgQ"}}})


(comment
  (user-info {:session session})
  (user-id {:session session}))
