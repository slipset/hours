(ns hours.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [hours.handler :refer :all]))

(deftest test-wrap-db
  (let [fn (wrap-add-db identity :foo)]
    (is (= {:params {:db :foo}} (fn {})))))
