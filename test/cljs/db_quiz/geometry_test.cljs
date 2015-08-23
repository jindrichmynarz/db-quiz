(ns db-quiz.geometry-test
  (:require [cljs.test :refer-macros [are deftest is testing]]
            [db-quiz.geometry :as geo]))

(deftest path-travelled
  (testing "Length of the generated path travelled equals to distance travelled"
    (let [; TODO: This can be generated with test.check
          path [[[0.1 0.1] [0.2 0.2]] [[0.8 0.8] [0.9 0.9]]]]
      (are [distance-travelled]
           (= (geo/path-length (geo/path-travelled path distance-travelled)) distance-travelled)
           0
           0.1
           0.2)))) 
