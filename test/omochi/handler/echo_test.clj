(ns omochi.handler.echo-test
  (:require [clojure.test :refer :all]
            [omochi.handler.echo :refer :all :as h]))

(def p1 {:name "name1" :pattern "pattern1" :response "response1"})
(def p2 {:name "name2" :pattern "pattern2" :response "response2"})
(def p3 {:name "name3" :pattern "I am (\\w+)\\." :response "Hi, \\1!"})
(def nj {:name "nj" :pattern "[Nn]ick\\s?[Jj]aguar" :response "nick_jaguar"})

(deftest format-pattern-test
  (testing "format pattern"
    (is (= (h/format-pattern p1)
           (str
            ":small_blue_diamond:*name:* `name1`\n"
            ":small_blue_diamond:*pattern:* `pattern1`\n"
            ":small_blue_diamond:*response:* `response1`\n"
            ":small_blue_diamond:*command for update:*\n"
            "```!echo update name1 pattern1 response1```"))))
  (testing "format patterns"
    (is (= (h/format-patterns `(~p1
                                ~p2))
           (str
            ":small_blue_diamond:*name:* `name1`\n"
            ":small_blue_diamond:*pattern:* `pattern1`\n"
            ":small_blue_diamond:*response:* `response1`\n"
            ":small_blue_diamond:*command for update:*\n"
            "```!echo update name1 pattern1 response1```"
            "\n"
            h/emoji-hr
            "\n"
            ":small_blue_diamond:*name:* `name2`\n"
            ":small_blue_diamond:*pattern:* `pattern2`\n"
            ":small_blue_diamond:*response:* `response2`\n"
            ":small_blue_diamond:*command for update:*\n"
            "```!echo update name2 pattern2 response2```"
            )))))

(deftest match-test
  (testing "match simple pattern"
    (is (= (h/match? p1 "pattern1")
           "pattern1")))
  (testing "match simple sub match pattern"
    (is (= (h/match? p1 "Apattern1B")
           "pattern1")))
  (testing "not match"
    (nil? (h/match? p1 "ABC"))))

(deftest response-for-test
  (testing "simple"
    (is (= (h/response-for p1 "Apattern1B")
           "response1")))
  (testing "regex"
    (is (= (h/response-for p3 "I am John.")
           "Hi, John!")))
  (testing "Nick Jaguar"
    (is (= (h/response-for nj "Nick Jaguar")
           "nick_jaguar"))))

