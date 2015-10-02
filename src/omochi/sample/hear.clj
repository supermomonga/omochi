(ns omochi.sample.hear)

(defn hear-handler
  "hello - reply with 'hello'"
  [{:keys [text username]}]
  (if (and (string? text) (re-find #"hello" text))
    (str "hello " username)))
