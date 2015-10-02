(ns omochi.sample.pingpong)

(defn ping-handler
  "jubot ping - reply with 'pong'"
  [{:keys [text message-for-me?]}]
  (if (and message-for-me? (= text "ping"))
    "pong"))
