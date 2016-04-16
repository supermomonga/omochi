(ns omochi.handler.emit-event
  "Handler to emit event"
  (:require [omochi.util :as util]
            [environ.core :refer [env]]
            [slacker.client :refer [emit!]]))

(defn handler
  [{:keys [channel user text]}]
  (if (and text (util/mention-to? (env :bot-id) text))
    (when-let [event (second (re-find #"^<@[^>]+>:?\s+emit!\s+(.+)$" text)) ]
      (emit! :slacker.client/send-message channel
             (format "OK, I'll emit `%s` event." event))
      (emit! (keyword "slacker.client" event)))))
