(ns hours.report-test
    (:require [hours.report :as sut]
              [clj-time.core :as t]
              [clj-time.format :as f]
              [hours.time :as time])
    (:use clojure.test))



(deftest get-week-start
  (let [now (t/now)
        mon (time/trunc-hours (time/prev-monday now))
        now-str (f/unparse (f/formatters :basic-date)now)]
    (is (= mon (sut/get-week-start ":this")))
    (is (= mon (sut/get-week-start now-str)))))

(run-all-tests)
