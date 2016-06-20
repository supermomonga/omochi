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
    (emit! :slacker.client/connect-bot api-token)
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
  (handle :bot-disconnected connect) ;; Auto reconnect
  (handle :connect-bot-error (fn [& args]
                               (Thread/sleep (* 1000 60 5))
                               (connect)))
  (connect)
  (await! :kill))

(defn -main
  [& args]
  (run))

