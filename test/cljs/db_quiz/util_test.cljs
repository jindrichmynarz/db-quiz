(ns db-quiz.util-test
  (:require [cljs.test :refer-macros [are deftest is testing]]
            [db-quiz.util :as util]))

(deftest shade-colour
  (testing "Comparing with reference implementation"
    (are [input-colour shade-percentage output-colour]
         (= (util/shade-colour input-colour shade-percentage) output-colour)
         "#fa0000" 0 "#fa0000" ; With 0 shade percentage, the colour must stay the same.
         "#fa0000" 30 "#ff4d4d"
         "#fa0000" -30 "#ae0000")))

