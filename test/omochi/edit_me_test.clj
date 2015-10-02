(ns omochi.edit-me-test
  (:require
    [omochi.edit-me :refer :all]
    [jubot.test :refer :all]
    [clojure.test :refer :all]))

(deftest test-your-first-handler
  (are [x y] (= x (your-first-handler y))
    nil   {}
    "bar" {:message-for-me? true :text "foo"}))
