(defproject hours "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
   :url "http://example.com/FIXME"
   :repl-options {:init-ns hours.handler}
  :main  ^:skip-aot hours.handler  
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [compojure "1.4.0"]
                 [clj-time "0.9.0"]
                 [hiccup "1.0.5"]
                 [vita-io/friend-oauth2 "0.1.4"]
                 [environ "0.3.1"]
                 [cheshire "5.5.0"]
                 [clj-http "2.0.0"]
                 [ring-mock "0.1.5"]
                 [hiccup-bootstrap "0.1.2"]]
   
  :plugins [[lein-ring "0.9.7"] [lein-environ "1.0.1"]]
   :ring {:handler hours.handler/app}
  :uberjar-name "hours-standalone.jar"
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}
   :uberjar {:aot :all}
   })
