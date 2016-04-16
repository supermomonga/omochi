(defproject omochi "0.1.0-SNAPSHOT"
  :description "A benri slack bot"
  :url "http://github.com/supermomonga/omochi"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [emiln/slacker "1.5.1"]
                 [environ "1.0.0"]
                 [cheshire "5.5.0"]
                 [clojail "1.0.6"]
                 [robert/hooke "1.3.0"]
                 [org.clojure/java.jdbc "0.6.0-alpha1"]
                 [org.xerial/sqlite-jdbc "3.8.11.2"]]
  :plugins [[lein-environ "1.0.0"]]
  :profiles {:uberjar {:aot :all}}
  :bootclasspath true
  :main omochi.core)
