(ns omochi.util
  "Omochi utilities for registering handler")


(defn append-timestamp [url]
  (str url "#" (System/currentTimeMillis)))

(defn ensure-fresh-image [text]
  (and text
       (clojure.string/replace text #"(https?:\/\/(?:[^ ]+)\.(?:jpg|jpeg|png|gif))" #(append-timestamp (last %)))))

(defn mention-to? [id text]
  (when-let [[_ to] (re-find #"^<@([^>]+)>:?\s+" text) ]
    (= to id)))

(defn user-id? [id]
  (and id
       (.startsWith id "U")))

