(ns hours.security
    (:require
      [friend-oauth2.workflow :as oauth2]
      [friend-oauth2.util     :refer [format-config-uri]]
      [environ.core           :refer [env]]
      [clj-http.client :as client]
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

(defn credential-fn [token]
  (let [userinfo (google-user-details (:access-token token))]
    (reset! current-user userinfo)
    {:identity token
     :user-info userinfo
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

(def friend-config
  {:allow-anon? true
   :workflows   [(oauth2/workflow
                  {:client-config client-config
                   :uri-config uri-config
                   :credential-fn credential-fn})
                   ]})

(defn logout []
  (reset! current-user {}))

(defn logged-in-user []
  @current-user)
