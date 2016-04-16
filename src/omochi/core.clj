(ns omochi.core
  (:require [slacker.client :refer [emit! await! handle with-stacktrace-log]]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [omochi.handler.yamabiko]
            [omochi.handler.echo]
            [omochi.handler.emit-event]
            [omochi.handler.clojure-playground]))

(defn connect []
  (if-let [api-token (env :slack-api-token)]
    (do
      (log/info "Establish connection.")
      (emit! :slacker.client/connect-bot api-token)
      (log/info "Wait for bot disconnect.")
      (await! :slacker.client/bot-disconnected)
      (log/warn "Bot disconnected.")
      (connect))
    (log/error "You need to set environment variable `SLACK_API_TOKEN`.")))

(defn run []
  (log/info "Omochi started.")
  (handle :message omochi.handler.yamabiko/handler)
  (handle :message omochi.handler.clojure-playground/handler)
  (handle :message omochi.handler.echo/handler)
  (handle :message omochi.handler.emit-event/handler)
  (handle :websocket-closed (fn [& args] (log/warn args)))
  (handle :bot-disconnected (fn [& args] (log/warn args)))
  (handle :websocket-errored (fn [& args] (log/error args)))
  (connect))

(defn -main
  [& args]
  (run))











