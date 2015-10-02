(ns omochi.sample.brain
  (:require
    [clojure.string :as str]
    [jubot.handler  :as handler]
    [jubot.brain    :as brain]))

(defn brain-handler
  "jubot set <key> <value> - store value to brain
   jubot get <key>         - restore value from brain"
  [{:keys [message-for-me?] :as arg}]
  (handler/regexp arg
    #"^set (.+?) (.+?)$" (fn [{[_ k v] :match}] (brain/set k v) "OK")
    #"^get (.+?)$"       (fn [{[_ k]   :match}] (brain/get k))
    #"^keys$"            (fn [& _] (str/join "," (brain/keys)))))
