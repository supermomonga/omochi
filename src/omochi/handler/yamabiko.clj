(ns omochi.handler.yamabiko
  "Yamabiko handler"
  (:require [slacker.client :refer [emit!]]))

(defn yamabiko-message [text]
  (when text
    (last (re-find #"!yamabiko (.+)" text))))

(defn handler
  [{:keys [channel user text]}]
  (when-let [message (yamabiko-message text)]
    (emit! :slacker.client/send-message channel
           (format "<@%s>: %s" user message))))

