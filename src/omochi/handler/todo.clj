(ns omochi.handler.todo
  "Echo handler"
  (:require [omochi.util :as util]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [slacker.client :refer [emit!]]
            [clj-slack.chat :as chat]
            [clojure.java.jdbc :as dbc]))

(def db {:subprotocol "sqlite"
         :subname (or (env :echo-db-path)
                      "target/todo.sqlite")})

(def ^:private active-todolist (ref {}))
(def ^:private active-todo-user (ref {}))
(def ^:private active-todo-cursor (ref {}))
(def ^:private active-message-ts (ref {}))

(defn db-init []
  (dbc/execute!
   db
   ["CREATE TABLE IF NOT EXISTS `todos` (id integer PRIMARY KEY AUTOINCREMENT, status integer DEFAULT 0 NOT NULL, user text NOT NULL, description text NOT NULL);"]))

(defn add! [user description]
  (dbc/insert! db :todos {:user user :description description}))

(defn add-to! [from to description]
  (dbc/insert! db :todos {:user to :description description}))

(defn todos [user status]
  (dbc/query db [(format "SELECT * FROM `todos` WHERE `user` = '%s' AND `status` = %s"
                         user
                         status)]))

(defn set-active-todolist [channel todolist]
  (dosync
   (alter active-todolist assoc channel todolist)))

(defn get-active-todolist [channel]
  (get @active-todolist channel))

(defn set-active-todo-user [channel user]
  (dosync
   (alter active-todo-user assoc channel user)))

(defn get-active-todo-user [channel]
  (get @active-todo-user channel))

(defn set-active-todo-cursor [channel n]
  (dosync
   (alter active-todo-cursor assoc channel n)))

(defn get-active-todo-cursor [channel]
  (if (not (get @active-todo-cursor channel))
    (set-active-todo-cursor channel 0))
  (get @active-todo-cursor channel))

(defn increment-active-todo-cursor [channel todos]
  (let [current (get-active-todo-cursor channel)
        cursor (+ 1 current)]
    (dosync
     (set-active-todo-cursor channel
              (if (>= cursor (count todos))
                0
                cursor)))))

(defn decrement-active-todo-cursor [channel todos]
  (let [current (get-active-todo-cursor channel)
        cursor (- current 1)]
    (dosync
     (set-active-todo-cursor channel
              (if (< cursor 0)
                (- (count todos) 1)
                cursor)))))

(defn set-active-message-ts [channel ts]
  (dosync
   (alter active-message-ts assoc channel ts)))

(defn get-active-message-ts [channel]
  (get @active-message-ts channel))

(defn done-todo [channel idx todo]
  (if (= (get-active-todo-cursor channel) idx)
    (assoc todo :status 1)
    todo))

(defn undone-todo [channel idx todo]
  (if (= (get-active-todo-cursor channel) idx)
    (assoc todo :status 0)
    todo))

(defn done-active-todo [channel todos]
  (let [updated-todos (map-indexed (partial done-todo channel) todos)]
    (set-active-todolist channel updated-todos)))

(defn undone-active-todo [channel todos]
  (let [updated-todos (map-indexed (partial undone-todo channel) todos)]
    (set-active-todolist channel updated-todos)))

(defn discard-changes [channel]
  (set-active-todolist channel (todos (get-active-todo-user channel) 0)))

(defn apply-changes [channel -todos]
  (doall
   (map (fn [{id :id status :status}]
          (dbc/update! db :todos {:status status} ["id = ?" id]))
        -todos))
  (set-active-todolist channel (todos (get-active-todo-user channel) 0))
  (set-active-todo-cursor channel 0))

(def help-br
  (clojure.string/join
   "\n"
   '("　")))

(def help-header
  (clojure.string/join
   "\n"
   `(
     ":mag_right: todo は、シンプルなタスク管理機能を提供します"
     ~help-br)))

(def help-message
  (clojure.string/join
   "\n"
   `(
     ~help-header
     ":large_orange_diamond: 使い方"
     "`!todo [command] [args]`"
     "*command*: コマンド名 (help|add|add-to|list|list-of)"
     "*args*: コマンドに対する引数"
     ~help-br
     ":large_orange_diamond: より詳細なヘルプ"
     ":small_orange_diamond: `!todo help`"
     ":question: このヘルプです"
     ":small_orange_diamond: `!todo add [description]`"
     ":question: 自分のTODOリストにタスク `[description]` を追加します"
     ":small_orange_diamond: `!todo add-to [user]`"
     ":question: ユーザ `[user]` のTODOリストにタスク `[description]` を追加します"
     ":small_orange_diamond: `!todo list`"
     ":question: 自分のTODOリストを表示し、操作することができます"
     ":small_orange_diamond: `!todo list-of [user]`"
     ":question: ユーザ `[user]` のTODOリストを表示し、操作することができます")))

(defn parse-args [text]
  (when text
    (or
     (when-let [match (re-find #"^!todo (help)" text)]
       (zipmap [:action] (rest match)))
     (when-let [match (re-find #"^!todo (add-to) ([^ ]+) (.+)" text)]
       (zipmap [:action :name :description] (rest match)))
     (when-let [match (re-find #"^!todo (add) (.+)" text)]
       (zipmap [:action :description] (rest match)))
     (when-let [match (re-find #"^!todo (list-of) (.+)" text)]
       (zipmap [:action :name] (rest match)))
     (when-let [match (re-find #"^!todo (list)" text)]
       (zipmap [:action] (rest match))))))

(defn add-action-buttons [token channel ts]
  (util/add-reaction token channel ts "arrow_up_small")
  (util/add-reaction token channel ts "arrow_down_small")
  (util/add-reaction token channel ts "white_square")
  (util/add-reaction token channel ts "ballot_box_with_check")
  (util/add-reaction token channel ts "negative_squared_cross_mark")
  (util/add-reaction token channel ts "white_check_mark"))

(defn format-reaction [todo]
  (case (:status todo)
    0 "white_small_square"
    1 "black_small_square"))

(defn format-description [cursor idx todo]
  (if (= idx cursor)
    (format ":point_right: %s" (:description todo))
    (:description todo)))

(defn format-todo [cursor idx todo]
  (format ":%s: %s"
          (format-reaction todo)
          (format-description cursor idx todo)))

(defn format-todos [cursor todos]
  (clojure.string/join "\n" (map-indexed
                             (fn [idx todo] (format-todo cursor idx todo))
                             todos)))

(def hr (apply str (repeat 10 ":wavy_dash:")))

(def in-list-description
  ":white_small_square: undone / :black_small_square: done / :negative_squared_cross_mark: cancel / :white_check_mark: apply")

(defn list-message [channel]
  (let [formatted-todos (format-todos
                         (get-active-todo-cursor channel)
                         (get-active-todolist channel))]
    (if (= formatted-todos "")
      ":tada: 全てのタスクが完了しました！"
      (format "%s\n　\n以下のボタンで操作することができます"
            formatted-todos))))

(defn help-handler [channel]
  (emit! :slacker.client/send-message channel
         help-message))

(defn list-handler [channel user]
  (when-let [todos (todos user 0)]
    (if (> (count todos) 0)
      (do
        (set-active-todolist channel todos)
        (set-active-todo-cursor channel 0)
        (set-active-todo-user channel user)
        (let [message (list-message channel)
              {ok :ok channel :channel ts :ts}
              (chat/post-message @util/conn channel message {:as_user "true"})]
          (when ok
            (set-active-message-ts channel ts)
            (add-action-buttons @util/token channel ts))))
      (emit! :slacker.client/send-message channel
             (format ":tada: Yay! you have done your all tasks!")))))

(defn update-list-handler [channel]
  (let [message (list-message channel)]
    (chat/update
     @util/conn
     (get-active-message-ts channel)
     channel
     message)))

(defn add-handler [channel user description]
  (when (add! user description)
    (emit! :slacker.client/send-message channel
           (format ":ok: New task added to %s." user))))

(defn add-to-handler [channel from to description]
  (when (add-to! from to description)
    (emit! :slacker.client/send-message channel
           (format ":ok: New task added to %s by %s." to from))))

(defn reaction-toggled-handler
  [{user :user reaction :reaction {channel :channel ts :ts} :item}]
  (when (not (= user (util/bot-id)))
    (let [user-name (:name (util/user-by :id user))]
      (if (= ts (get-active-message-ts channel))
        (case reaction
          "arrow_up_small"
          (do (decrement-active-todo-cursor channel (get-active-todolist channel))
              (update-list-handler channel))
          "arrow_down_small"
          (do (increment-active-todo-cursor channel (get-active-todolist channel))
              (update-list-handler channel))
          "white_large_square"
          (do (undone-active-todo channel (get-active-todolist channel))
              (update-list-handler channel))
          "ballot_box_with_check"
          (do (done-active-todo channel (get-active-todolist channel))
              (update-list-handler channel))
          "negative_squared_cross_mark"
          (do (discard-changes channel)
              (update-list-handler channel))
          "white_check_mark"
          (do (apply-changes channel (get-active-todolist channel))
              (update-list-handler channel))
          nil)))))

(defn handler
  [{:keys [channel user text ts]}]
  (db-init)
  (when (and text (util/user-id? user))
    (let [user (:name (util/user-by :id user))]
      (if-let [{:keys [action name description]} (parse-args text)]
      (case action
        "help" (help-handler channel)
        "list" (list-handler channel user)
        "list-of" (list-handler channel name)
        "add" (add-handler channel user description)
        "add-to" (add-to-handler channel user name description))))))


