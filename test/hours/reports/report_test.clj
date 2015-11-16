(ns hours.reports.report-test
    (:require [hours.reports.report :as sut]
              [clj-time.core :as time-core]
              [clojure.test :refer :all]))

(deftest test-week-total
  (is (= 8 (sut/period-total [{:total 1}
                              {:total 1}
                              {:total 1}
                              {:total 1}
                              {:total 1}
                              {:total 1}
                              {:total 2}]))))

(deftest test-decorate
  (is (= #{:report :client-id
           :clients :projects
           :period :day-totals}  (->> (sut/decorate [(time-core/now) (time-core/now)] nil {})
                                      (keys)
                                      (into #{})))))

(deftest test-add-week-total
  (let [days [{:total 1} {:total 1} {:total 1}]
        result (sut/add-period-total days)]
    (is (= 4 (count result))
        (= 3 (:total (last result))))))

(deftest summarize
  (is (= #{:total :day} (->> (sut/summarize [])
                             (keys)
                             (into #{})))))
