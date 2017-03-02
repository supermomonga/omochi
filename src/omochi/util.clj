(ns omochi.util
  "Omochi utilities for registering handler"
  (:require
   [org.httpkit.client :as http]
   [clojure.data.json :as json]
   [slacker.converters :refer [string->keyword string->slack-json]]))

(def conn (ref {}))

(def token (ref ""))

(def users (ref '()))

(def -bot-name (ref ""))

(defn set-token [new-token]
  (dosync
   (ref-set token new-token)))

(defn set-connection [api-url token]
  (dosync
   (ref-set conn {:api-url api-url :token token})))

(defn post-message [token channel ts reaction]
  (let [options {:form-params {:token token
                               :channel channel
                               :name reaction
                               :timestamp ts}}]
    (:body (deref (http/post "https://slack.com/api/reactions.add" options)))))

(defn add-reaction [token channel ts reaction]
  (let [options {:form-params {:token token
                               :channel channel
                               :name reaction
                               :timestamp ts}}]
    (:body (deref (http/post "https://slack.com/api/reactions.add" options)))))

(defn update-user-list [token]
  (let [data
         (-> (format "https://slack.com/api/users.list?token=%s" token)
             http/get
             deref
             :body
             (json/read-str :key-fn string->keyword)
             :members)]
    (dosync
     (ref-set users data))))

(defn user-from-name [name]
  (first
   (filter
    (fn [u] (= (:name u) name))
    @users)))

(defn user-by [k v]
  (first
   (filter
    (fn [u] (= (k u) v))
    @users)))

(defn set-bot-name [name]
  (dosync
   (ref-set -bot-name name)))

(defn bot-name []
  @-bot-name)

(defn bot-id []
  (:id (user-by :name (bot-name))))

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


