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

(defn handler-eval-clojure
  [{:keys [channel user text]}]
  (when (and
         (= \( (first text))
         (sexp? text))
    (when-let [response
               (let [result (eval-request text)]
                 (or (-> result :result last)
                     result))]
      (emit! :slacker.client/send-message channel
             (format "```%s```" response)))))

(defn handler-yamabiko
  [{:keys [channel user text]}]
  (if text
    (when-let [text (last (re-find #"!yamabiko (.+)" text))]
      (emit! :slacker.client/send-message channel
             (format "<@%s>: %s" user text)))))

(def simple-matcher-rules
  {#"^ping"                       "pong"
   #"^目の錯覚(って怖いねん)?$"   "http://i.gyazo.com/552f4577e9bb63c18766a705fc63f553.jpg"
   #"^目の錯覚(って怖いですね)?$" "http://i.gyazo.com/4383943b54188d8bcf185456516186b8.jpg"
   #"^!ppp"                       "PonPonPain"
   #"^!b"                         "便利"
   #"^!bs"                        "便利そう"
   #"^!f"                         "不便"
   #"^!fs"                        "不便そう"
   #"^!no"                        "http://d.pr/i/15zJh.png"
   #"^ぬるオーラ"                 "http://d.pr/i/15zJh.png"
   #"^行けたら行く"               "http://d.pr/i/11Q6l.png"
   #"^!snttm"                     "http://d.pr/i/11H8B.png"
   #"^汁なし担々麺"               "http://d.pr/i/11H8B.png"})

(defn append-timestamp [url]
  (str url "?" (System/currentTimeMillis)))

(defn ensure-fresh-image [url]
  (if (re-find #"\.(jpg|jpeg|png|gif)$" url)
    (append-timestamp url)
    url))

(defn simple-matcher [text rules]
  (when text
    (let [rules (filter #(-> % key (re-find text)) rules) ]
      (when (not (empty? rules))
        (-> rules shuffle first val ensure-fresh-image)))))

(defn handler-simple-matcher
  [{:keys [channel user text]}]
  (when-let [res (simple-matcher text simple-matcher-rules)]
    (emit! :slacker.client/send-message channel res)))

(defn idols []
  (-> "id2hash.json" io/resource io/reader))

(defn connect []
  (if-let [api-token (env :slack-api-token)]
    (do
      (log/info "Omochi started.")
      (emit! :slacker.client/connect-bot api-token)
      (await! :slacker.client/bot-disconnected))
    (log/error "You need to set environment variable `SLACK_API_TOKEN`.")))

(defn run []
  (handle :message handler-simple-matcher)
  (handle :message handler-yamabiko)
  (handle :message handler-eval-clojure)
  (handle :websocket-closed (fn [& args] (log/warn args) (connect)))
  (handle :websocket-errored (fn [& args] (log/error args) (connect)))
  (connect))

(defn -main
  [& args]
  (run))

