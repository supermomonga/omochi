(ns sample-slacker-pingpong.core
  (:require [clojure.string :refer [lower-case]]
            [slacker.client :refer [emit! await! handle with-stacktrace-log]]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]))

(defn ping-pong
  "ping-pong"
  [{:keys [channel text]}]
  (when (= text "ping")
    (emit! :slacker.client/send-message channel "pong")))

(defn -main
  [& args]
  (handle :message ping-pong)
  (if-let [api-token (env :slack-api-token)]
    (do
      (log/info (str "API TOKEN : " api-token))
      (emit! :slacker.client/connect-bot api-token)
      (await! :slacker.client/bot-disconnected))
    (log/error "You need to set environment variable `SLACK_API_TOKEN`.")))
