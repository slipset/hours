(ns hours.client
    (:require [yesql.core :refer [defqueries]]))

(defqueries "hours/client.sql")
