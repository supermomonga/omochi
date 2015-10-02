(ns omochi.sample.another-bot
  (:require [jubot.adapter :refer [out]]))

(def ^:const BOT_NAME "foo")
(def ^:const BOT_ICON (some-> (System/getenv "BOT_URL") (str "/static/icon/jubot.png")))

(defn echo-handler
  [{:keys [to text]}]
  (when (= BOT_NAME to)
    (out text :as BOT_NAME :icon-url BOT_ICON)
    nil))
