(ns sample-slacker-pingpong.core
  (:require [clojure.string :refer [lower-case]]
            [slacker.client :refer [emit! await! handle with-stacktrace-log] :as s]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]))

(defn yamabiko
  [{:keys [channel user text]}]
  (emit! :slacker.client/send-message channel
         (format "<@%s>: " user text)))

(defn ping-pong
  "ping-pong"
  [{:keys [channel user text]}]
  (when (= text "ping")
   (emit! :slacker.client/send-message channel "pong")))

(defn run []
  (handle :message ping-pong)
  (handle :message yamabiko)
  (if-let [api-token (env :slack-api-token)]
    (do
      (log/info "Omochi started.")
      (emit! :slacker.client/connect-bot api-token)
      (println (clojure.core.async/<!! (slacker.client/emit-with-feedback! :add 1 2 3)))
      (await! :slacker.client/bot-disconnected))
    (log/error "You need to set environment variable `SLACK_API_TOKEN`.")))

(defn -main
  [& args]
  (run))

