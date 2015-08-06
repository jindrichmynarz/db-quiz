(ns db-quiz.fixtures.boards)

(def board-3-player-1-wins
  {[1 1] {:ownership :player-1
          :neighbours #{[2 1] [2 2]}
          :sides #{:a :b}}
   [2 1] {:ownership :player-1
          :neighbours #{[1 1] [2 2] [3 1] [3 2]}
          :sides #{:a}}
   [2 2] {:ownership :player-2
          :neighbours #{[1 1] [2 1] [3 2] [3 3]}
          :sides #{:b}}
   [3 1] {:ownership :player-2
          :neighbours #{[2 1] [3 2]}
          :sides #{:a :c}}
   [3 2] {:ownership :player-1
          :neighbours #{[2 1] [2 2] [3 1] [3 3]}
          :sides #{:c}}
   [3 3] {:ownership :missed
          :neighbours #{[2 2] [3 2]}
          :sides #{:b :c}}})

(def board-3-player-2-wins
  {[1 1] {:ownership :player-1
          :neighbours #{[2 1] [2 2]}
          :sides #{:a :b}}
   [2 1] {:ownership :player-1
          :neighbours #{[1 1] [2 2] [3 1] [3 2]}
          :sides #{:a}}
   [2 2] {:ownership :player-1
          :neighbours #{[1 1] [2 1] [3 2] [3 3]}
          :sides #{:b}}
   [3 1] {:ownership :player-2
          :neighbours #{[2 1] [3 2]}
          :sides #{:a :c}}
   [3 2] {:ownership :player-2
          :neighbours #{[2 1] [2 2] [3 1] [3 3]}
          :sides #{:c}}
   [3 3] {:ownership :player-2
          :neighbours #{[2 2] [3 2]}
          :sides #{:b :c}}})

(def board-4-player-1-wins
  {[1 1] {:ownership :player-2
          :neighbours #{[2 1] [2 2]}
          :sides #{:a :b}}
   [2 1] {:ownership :player-1
          :neighbours #{[1 1] [2 2] [3 1] [3 2]}
          :sides #{:a}}
   [2 2] {:ownership :player-2
          :neighbours #{[1 1] [2 1] [3 2] [3 3]}
          :sides #{:b}}
   [3 1] {:ownership :player-2
          :neighbours #{[2 1] [3 2] [4 1] [4 2]}
          :sides #{:a}}
   [3 2] {:ownership :player-1
          :neighbours #{[2 1] [2 2] [3 1] [3 3] [4 2] [4 3]}
          :sides #{}}
   [3 3] {:ownership :player-1
          :neighbours #{[2 2] [3 2] [4 3] [4 4]}
          :sides #{:b}}
   [4 1] {:ownership :player-1
          :neighbours #{[3 1] [4 2]}
          :sides #{:a :c}}
   [4 2] {:ownership :player-1
          :neighbours #{[4 1] [3 1] [3 2] [4 3]}
          :sides #{:c}}
   [4 3] {:ownership :player-2
          :neighbours #{[4 2] [3 2] [3 3] [4 4]}
          :sides #{:c}}
   [4 4] {:ownership :player-1
          :neighbours #{[4 3] [3 3]}
          :sides #{:b :c}}})

(def board-4-player-2-wins
  {[1 1] {:ownership :player-2
          :neighbours #{[2 1] [2 2]}
          :sides #{:a :b}}
   [2 1] {:ownership :player-1
          :neighbours #{[1 1] [2 2] [3 1] [3 2]}
          :sides #{:a}}
   [2 2] {:ownership :player-1
          :neighbours #{[1 1] [2 1] [3 2] [3 3]}
          :sides #{:b}}
   [3 1] {:ownership :player-1
          :neighbours #{[2 1] [3 2] [4 1] [4 2]}
          :sides #{:a}}
   [3 2] {:ownership :player-2
          :neighbours #{[2 1] [2 2] [3 1] [3 3] [4 2] [4 3]}
          :sides #{}}
   [3 3] {:ownership :player-2
          :neighbours #{[2 2] [3 2] [4 3] [4 4]}
          :sides #{:b}}
   [4 1] {:ownership :player-2
          :neighbours #{[3 1] [4 2]}
          :sides #{:a :c}}
   [4 2] {:ownership :player-2
          :neighbours #{[4 1] [3 1] [3 2] [4 3]}
          :sides #{:c}}
   [4 3] {:ownership :player-1
          :neighbours #{[4 2] [3 2] [3 3] [4 4]}
          :sides #{:c}}
   [4 4] {:ownership :player-1
          :neighbours #{[4 3] [3 3]}
          :sides #{:b :c}}})
