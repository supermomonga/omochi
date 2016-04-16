(ns omochi.handler.clojure-playground
  "Handler to eval clojure code"
  (:require [omochi.util :as util]
            [slacker.client :refer [emit!]]
            [clojail.testers :refer [secure-tester-without-def blanket secure-tester]]
            [clojail.core :refer [sandbox]]
            [clojure.stacktrace :refer [root-cause]])
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


(defn handler
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
