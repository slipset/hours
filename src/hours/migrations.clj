(ns hours.migrations
    (:require [ragtime.jdbc :as jdbc]
              [ragtime.repl :as repl]
              [environ.core :refer [env]]))

(def config
  {:datastore   (jdbc/sql-database {:connection-uri (env :jdbc-database-url)}) 
   :migrations (jdbc/load-resources "migrations")}) 

(defn migrate []
  (repl/migrate config))
