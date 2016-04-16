(ns omochi.util-test
  (:require [clojure.test :refer :all]
            [omochi.util :refer :all :as u]))

(deftest collect-mention-test
  (testing "collect id"
    (true? (u/mention-to? "id" "<@id>: hi"))))

(deftest incollect-mention-test
  (testing "incollect id"
    (nil? (u/mention-to? "me" "<@id>: hi"))))
