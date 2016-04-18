(ns omochi.handler.clojure-playground
  "Handler to eval clojure code"
  (:require [omochi.util :as util]
            [clojure.tools.logging :as log]
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

(defn should-eval? [text]
  (and
   (= \( (first text))
   (sexp? text)))


(defn handler
  [{:keys [channel user text]}]
  (when (should-eval? text)
    (let [{expr :expr error :error message :message [stdout result] :result} (eval-request text)]
      (let [stdout (if (empty? (str stdout)) nil stdout)]
        (when-let [text
                   (cond
                     (and stdout result) (format "```%s\n=> %s```" stdout result)
                     (or stdout result) (format "```%s```" (or stdout result))
                     (and error message) (format ":warning: `%s`" message))]
          (emit! :slacker.client/send-message channel text))))))

