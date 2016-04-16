(ns omochi.util
  "Omochi utilities for registering handler")


(defn append-timestamp [url]
  (str url "?" (System/currentTimeMillis)))

(defn ensure-fresh-image [url]
  (if (re-find #"\.(jpg|jpeg|png|gif)$" url)
    (append-timestamp url)
    url))

(defn mention-to? [id text]
  (when-let [[_ to] (re-find #"^<@([^>]+)>:?\s+" text) ]
    (= to id)))


