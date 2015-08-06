(ns db-quiz.logic-test
  (:require [cljs.test :refer-macros [are deftest is testing]]
            [db-quiz.logic :as logic]
            [db-quiz.fixtures.boards :as boards]))

(deftest find-winner
   (testing "finding correct winner"
     (are [board winner]
          (= (-> board logic/find-winner :player) winner)
          boards/board-3-player-1-wins :player-1
          boards/board-3-player-2-wins :player-2
          boards/board-4-player-1-wins :player-1
          boards/board-4-player-2-wins :player-2))) 
