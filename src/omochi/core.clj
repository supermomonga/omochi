(ns omochi.core
  (:require [clojure.string :refer [lower-case]]
            [slacker.client :refer [emit! await! handle with-stacktrace-log] :as s]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [clojail.testers :refer [secure-tester-without-def blanket secure-tester]]
            [clojail.core :refer [sandbox]]
            [cheshire.core :refer [parse-stream]]
            [clojure.stacktrace :refer [root-cause]]
            [clojure.java.io :as io])
  (:import java.io.StringWriter
           java.util.concurrent.TimeoutException))

(defn eval-form [form sbox]
  (with-open [out (StringWriter.)]
    (let [result (sbox form {#'*out* out})]
      {:expr form
       :result [out result]})))

(defn eval-string [expr sbox]
  (let [form (binding [*read-eval* false] (read-string expr))]
    (eval-form form sbox)))

(def jail-tester
  (conj secure-tester-without-def (blanket "omochi" "slacker")))

(def sb (sandbox
         jail-tester
         :timeout 3000
         :init '(do (require '[clojure.repl :refer [doc source]]))))

(defn eval-request [expr]
  (try
    (eval-string expr sb)
    (catch TimeoutException _
      {:error true :message "Execution Timed Out!"})
    (catch Exception e
      {:error true :message (str (root-cause e))})))

(defn sexp? [text]
  (let [expr (try
               (read-string text)
               (catch RuntimeException e nil))]
    (and (sequential? expr)
         (not (string? expr)))))

(defn eval-clojure
  [{:keys [channel user text]}]
  (when (sexp? text)
    (when-let [response
               (let [result (eval-request text)]
                 (or (-> result :result last)
                     result))]
      (emit! :slacker.client/send-message channel
             (format "```%s```" response)))))

(defn yamabiko
  [{:keys [channel user text]}]
  (if text
    (when-let [text (last (re-find #"!yamabiko (.+)"))]
      (emit! :slacker.client/send-message channel
             (format "<@%s>: %s" user text)))))

(defn simple-matcher
  [{:keys [channel user text]}]
  (when-let [res (condp = text
                   "ping"         "pong"
                   "!ppp"         "PonPonPain"
                   "!b"           "便利"
                   "!bs"          "便利そう"
                   "!f"           "不便"
                   "!fs"          "不便そう"
                   "!no"          "http://d.pr/i/15zJh.png"
                   "ぬるオーラ"   "http://d.pr/i/15zJh.png"
                   "行けたら行く" "http://d.pr/i/11Q6l.png"
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

