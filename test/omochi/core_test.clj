(ns omochi.core-test
  (:require [clojure.test :refer :all]
            [omochi.core :refer :all]))

(deftest collect-mention-test
  (testing "collect id"
    (true? (omochi.core/mention-to? "id" "<@id>: hi"))))

(deftest incollect-mention-test
  (testing "incollect id"
    (nil? (omochi.core/mention-to? "me" "<@id>: hi"))))
