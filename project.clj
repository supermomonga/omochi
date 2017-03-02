(defproject omochi "0.1.0-SNAPSHOT"
  :description "A benri slack bot"
  :url "http://github.com/supermomonga/omochi"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [emiln/slacker "1.6.0"]
                 [org.julienxx/clj-slack "0.5.4"]
                 [environ "1.0.0"]
                 [cheshire "5.5.0"]
                 [clojail "1.0.6"]
                 [robert/hooke "1.3.0"]
                 [http-kit "2.1.19"]
                 [org.clojure/java.jdbc "0.6.0-alpha1"]
                 [org.postgresql/postgresql "9.4-1206-jdbc42"]]
  :plugins [[lein-environ "1.0.0"]]
  :min-lein-version "2.0.0"
  :uberjar-name "omochi-standalone.jar"
  :profiles {:uberjar {:aot :all}}
  :bootclasspath true
  :main omochi.core)
