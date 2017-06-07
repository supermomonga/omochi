(ns omochi.handler.tofu
  "Tofu handler"
  (:require [omochi.util :as util]
            [slacker.client :refer [emit!]]))

(defn contain-tofu? [text]
  (clojure.string/includes? "\b"))

(defn remove-tofu [text]
  (clojure.string/replace text "\b" ""))

(defn handler
  [{:keys [channel user text]}]
  (when (contain-tofu? text)
    (let [response (remove-tofu text)]
        (emit! :slacker.client/send-message channel
           (util/ensure-fresh-image response)))))
