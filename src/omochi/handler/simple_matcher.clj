(ns omochi.handler.simple-matcher
  "Simple matcher handler"
  (:require [omochi.util :as util]
            [slacker.client :refer [emit!]]))

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

(defn simple-matcher [text rules]
  (when text
    (let [rules (filter #(-> % key (re-find text)) rules) ]
      (when (not (empty? rules))
        (-> rules rand-nth val util/ensure-fresh-image)))))

(defn handler
  [{:keys [channel user text]}]
  (when-let [res (simple-matcher text simple-matcher-rules)]
    (emit! :slacker.client/send-message channel res)))
