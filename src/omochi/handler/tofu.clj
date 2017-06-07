(ns omochi.handler.tofu
  "Tofu handler"
  (:require [omochi.util :as util]
            [slacker.client :refer [emit!]]))

(defn include-tofu? [text]
  (clojure.string/includes? text "\b"))

(defn remove-tofu [text]
  (clojure.string/replace text "\b" ""))

(defn handler
  [{:keys [channel user text]}]
  (when (include-tofu? text)
    (let [response (remove-tofu text)]
        (emit! :slacker.client/send-message channel
           (util/ensure-fresh-image response)))))
