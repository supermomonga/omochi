(defproject omochi "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [emiln/slacker "1.5.1"]
                 [environ "1.0.0"]
                 [cheshire "5.5.0"]
                 [robert/hooke "1.3.0"]]
  :plugins [[lein-environ "1.0.0"]]
  :profiles {:uberjar {:aot :all}}
  :bootclasspath true
  :main omochi.core)
