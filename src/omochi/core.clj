(ns omochi.core
  (:require [clojure.string :refer [lower-case]]
            [slacker.client :refer [emit! await! handle with-stacktrace-log] :as s]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [cheshire.core :refer [parse-stream]]
            [clojure.java.io :as io]))

(defn yamabiko
  [{:keys [channel user text]}]
  (when-let [text (last (re-find #"!yamabiko (.+)" text))]
    (emit! :slacker.client/send-message channel
           (format "<@%s>: %s" user text))))

(defn simple-matcher
  [{:keys [channel user text]}]
  (when-let [res (condp = text
                   "ping"       "pong"
                   "!ppp"       "PonPonPain"
                   "!b"         "便利"
                   "!bs"        "便利そう"
                   "!f"         "不便"
                   "!fs"        "不便そう"
                   "!no"        "http://d.pr/i/15zJh.png"
                   "ぬるオーラ" "http://d.pr/i/15zJh.png"
                   nil
                   )]
    (emit! :slacker.client/send-message channel res)))

(defn idols []
  (-> "id2hash.json" io/resource io/reader))

(defn run []
  (handle :message simple-matcher)
  (handle :message yamabiko)
  (handle :message eval-clojure)
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

