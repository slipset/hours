(ns hours.reports.routes
    (:require
      [ring.util.response :refer [response redirect header]]
      [compojure.core :refer :all]
      [hours.reports.layout :as layout]
      [hours.reports.report :as report]))

(defroutes report-routes
  (GET "/by-week" r (redirect "/report/by-week/:all/:this"))
  (GET "/by-week/:client-id/:date" [client-id date user-id db] (response (layout/display-weekly-report
                                                                          (report/get-weekly-report db user-id client-id date)))))
