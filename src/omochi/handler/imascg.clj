(ns omochi.handler.imascg
  "WIP"
  (:require [slacker.client :refer [emit!]]
            [clojure.java.io :as io]))

(defn idols []
  (-> "id2hash.json" io/resource io/reader))

