(ns omochi.handler.echo
  "Echo handler"
  (:require [omochi.util :as util]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [slacker.client :refer [emit!]]
            [clojure.java.jdbc :as dbc]))

(def db {:subprotocol "sqlite"
         :subname (or (env :echo-db-path)
                      "target/echo.sqlite")})

(defn db-init []
  (dbc/execute! db ["CREATE TABLE IF NOT EXISTS `patterns` (name text PRIMARY KEY NOT NULL,pattern text NOT NULL,response text NOT NULL);"]))

(defn normalize-slack-formatted-urls
  [text]
  (and text (clojure.string/replace text #"<(https?:\/\/[^>]+?)>" #(last %1))))

(defn parse-args [text]
  (when text
    (or
     (when-let [match (re-find #"^!echo[- ](help)$" text)]
       (zipmap [:action :help-topic] (concat (rest match) '("default"))))
     (when-let [match (re-find #"^!echo[- ](help) (create|update|show|list|find|delete|all)" text)]
       (zipmap [:action :help-topic] (rest match)))
     (when-let [match (re-find #"(?s)^!echo[- ](create|update) ([^\s]+) ([^\s]+) (.+)" text)]
       (zipmap [:action :name :pattern :response] (rest match)))
     (when-let [match (re-find #"^!echo[- ](show|delete) ([^ ]+)" text)]
       (zipmap [:action :name] (rest match)))
     (when-let [match (re-find #"(?s)^!echo[- ](find) (.+)" text)]
       (zipmap [:action :message] (rest match)))
     (when-let [match (re-find #"^!echo[- ](list)" text)]
       (zipmap [:action] (rest match))))))

(def help-br
  (clojure.string/join
   "\n"
   '("　")))

(def help-header
  (clojure.string/join
   "\n"
   `(
     ":mag_right: echo は、予め設定したパターンにマッチする発言に対して、特定の返答を返すための機能です。"
     ~help-br)))

(def help-default
  (clojure.string/join
   "\n"
   `(
     ~help-header
     ":large_orange_diamond: 使い方"
     "`!echo [command] [args]`"
     "*command*: コマンド名 (help|create|update|show|list|find|delete)"
     "*args*: コマンドに対する引数"
     ~help-br
     ":large_orange_diamond: より詳細なヘルプ"
     "より詳細なヘルプを表示するには以下のコマンドを使用してください"
     ":point_right: `!echo help` このヘルプです"
     ":point_right: `!echo help all` ヘルプ全文を表示"
     ":point_right: `!echo help create` 作成コマンド"
     ":point_right: `!echo help update` 変更コマンド"
     ":point_right: `!echo help show` 表示コマンド"
     ":point_right: `!echo help list` 列挙コマンド"
     ":point_right: `!echo help find` 検索コマンド"
     ":point_right: `!echo help delete` 削除コマンド")))

(def help-create
  (clojure.string/join
   "\n"
   '(
     ":large_orange_diamond: create コマンド"
     "新しく反応パターンを登録します"
     "`!echo create [name] [pattern] [response]`"
     "*name*: 一意の反応パターン名を指定します"
     "*pattern*: パターンを正規表現で指定します"
     "*response*: 返答の内容を指定します"
     "例:point_right: `!echo create test1 ^ping$ pong`"
     "例:point_right: `!echo create test2 ^hello$ world`")))

(def help-update
  (clojure.string/join
   "\n"
   '(
     ":large_orange_diamond: update コマンド"
     "既存の反応パターンの内容を更新します"
     "`!echo update [name] [pattern] [response]`"
     "*name*: 内容を変更したい反応パターンの名前を指定します"
     "*pattern*: パターンを正規表現で指定します"
     "*response*: 返答の内容を指定します"
     "例:point_right: `!echo update test1 ^ping$ pong!`"
     "例:point_right: `!echo update test2 ^hello$ world!`")))

(def help-show
  (clojure.string/join
   "\n"
   '(
     ":large_orange_diamond: show コマンド"
     "既存の反応パターンの定義内容を表示します"
     "`!echo show [name]`"
     "*name*: 定義内容を表示したい反応パターンの名前を指定します"
     "例:point_right: `!echo show test1`"
     "例:point_right: `!echo show test2`"
     )))

(def help-list
  (clojure.string/join
   "\n"
   '(
     ":large_orange_diamond: list コマンド"
     "既存の反応パターンの定義内容を表示します"
     "`!echo list`"
     "*name*: 全ての反応パターンの名前を列挙します"
     )))

(def help-find
  (clojure.string/join
   "\n"
   '(
     ":large_orange_diamond: find コマンド"
     "指定したテキストがどの反応パターンにマッチするか調べます"
     "`!echo show [text]`"
     "*text*: マッチを試みたいテキストを指定します"
     "例:point_right: `!echo find ping`"
     "例:point_right: `!echo find hello`"
     )))

(def help-delete
  (clojure.string/join
   "\n"
   '(
     ":large_orange_diamond: delete コマンド"
     "既存の反応パターンの定義を削除します"
     "`!echo delete [name]`"
     "*delete*: 削除したい反応パターンの名前を指定します"
     "例:point_right: `!echo delete test1`"
     "例:point_right: `!echo delete test2`"
     )))

(def help-all
  (clojure.string/join
   "\n"
   `(
     ~help-header
     ~help-create
     ~help-br
     ~help-update
     ~help-br
     ~help-show
     ~help-br
     ~help-find
     ~help-br
     ~help-delete
     )))

(defn help
  [topic]
  (log/info (str "topic:" topic))
  (condp = (keyword topic)
    :default help-default
    :create help-create
    :update help-update
    :list help-list
    :show help-show
    :find help-find
    :delete help-delete
    :all help-all))

(def emoji-hr
  (apply str (repeat 20 ":sushi:")))

(defn match?
  "Check if the pattern is match to the given text"
  [pattern text]
  (re-find (re-pattern (:pattern pattern)) text))

(defn format-pattern
  [pattern]
  (let [{:keys [name pattern response]} pattern]
    (format (str
             ":small_blue_diamond:*name:* `%s`\n"
             ":small_blue_diamond:*pattern:* `%s`\n"
             ":small_blue_diamond:*response:* `%s`\n"
             ":small_blue_diamond:*command for update:*\n```!echo update %s %s %s```"
             )
            name pattern response name pattern response)))

(defn format-patterns
  [patterns]
  (clojure.string/join (format "\n%s\n" emoji-hr) (map format-pattern patterns)))

(defn find-all-by-pattern
  [text patterns]
  (filter #(match? % text) patterns))

(defn find-by-pattern
  [text patterns]
  (first (find-all-by-pattern text patterns)))

(defn show [name]
  (format-patterns (dbc/query db ["SELECT * FROM `patterns` WHERE name = ? LIMIT 1" name])))

(defn list []
  (clojure.string/join ", "
                       (map #(format "`%s`" (:name %))
                            (dbc/query db ["SELECT * FROM `patterns`"]))))

(defn create! [name pattern response]
  (if (empty? (show name))
    (when (dbc/insert! db :patterns {:name name :pattern pattern :response response})
      (format ":ok: `%s` created." name))
    (format ":warning: `%s` already exists." name)))

(defn update! [name pattern response]
  (if (empty? (show name))
    (format ":warning: `%s` is not exists." name)
    (when (dbc/update! db :patterns {:pattern pattern :response response} ["name = ?" name])
      (format ":ok: `%s` updated." name))))

(defn delete! [name]
  (if (empty? (show name))
    (format ":warning: `%s` is not exists." name)
    (when (dbc/delete! db :patterns ["name = ?" name])
      (format ":ok: `%s` deleted." name))))

(defn apply-crud!
  [action name pattern response message help-topic]
  (condp = (keyword action)
    :help (help help-topic)
    :create (create! name pattern response)
    :show (show name)
    :list (list)
    :find (format-patterns (find-all-by-pattern message (dbc/query db ["SELECT * FROM `patterns`"])))
    :update (update! name pattern response)
    :delete (delete! name)))

(defn handler
  [{:keys [channel user text]}]
  (db-init)
  (when (and text
             (util/user-id? user))
    (if-let [{:keys [action name pattern response message help-topic]} (parse-args text)]
      (when-let [res (apply-crud!
                      action
                      name
                      (normalize-slack-formatted-urls pattern)
                      (normalize-slack-formatted-urls response)
                      (normalize-slack-formatted-urls message)
                      help-topic)]
        (emit! :slacker.client/send-message channel (str res)))
      (let [patterns (shuffle (dbc/query db ["SELECT * FROM `patterns`"]))]
        (when-let [match (find-by-pattern text patterns)]
          (emit! :slacker.client/send-message channel (util/ensure-fresh-image (:response match))))))))






