(ns omochi.sample.brain-test
  (:require
    [omochi.sample.brain :refer :all]
    [jubot.test :refer :all]
    [clojure.test :refer :all]))

(deftest test-brain-handler
  (with-test-brain
    (are [x y] (= x (brain-handler
                      (merge {:message-for-me? true} y)))
      nil   {}
      "OK"  {:text "set foo bar"}
      "bar" {:text "get foo"}
      nil   {:text "get bar"}
      "foo" {:text "keys"})))
