(ns omochi.handler.echo-test
  (:require [clojure.test :refer :all]
            [omochi.handler.echo :refer :all :as h]))

(deftest format-pattern-test
  (testing "format pattern"
    (is (= (h/format-pattern {:name "name1" :pattern "pattern1" :response "response1"})
           (str
            ":small_blue_diamond:*name:* `name1`\n"
            ":small_blue_diamond:*pattern:* `pattern1`\n"
            ":small_blue_diamond:*response:* `response1`\n"
            ":small_blue_diamond:*command for update:*\n"
            "```!echo name1 pattern1 response1```"))))
  (testing "format patterns"
    (is (= (h/format-patterns '({:name "name1" :pattern "pattern1" :response "response1"}
                                {:name "name2" :pattern "pattern2" :response "response2"}))
           (str
            ":small_blue_diamond:*name:* `name1`\n"
            ":small_blue_diamond:*pattern:* `pattern1`\n"
            ":small_blue_diamond:*response:* `response1`\n"
            ":small_blue_diamond:*command for update:*\n"
            "```!echo name1 pattern1 response1```"
            "\n"
            (h/emoji-hr)
            "\n"
            ":small_blue_diamond:*name:* `name2`\n"
            ":small_blue_diamond:*pattern:* `pattern2`\n"
            ":small_blue_diamond:*response:* `response2`\n"
            ":small_blue_diamond:*command for update:*\n"
            "```!echo name2 pattern2 response2```"
            )))))
