(ns db-quiz.config)

(defonce config
  {; Number of hexagons on one side of the triangle
   :board-size 7
   :colours {:active "#eeeeee" 
             :default "#bcbcbc"
             :hover "#dedede"
             :missed "#333333"
             :player-1 "#fc4349"
             :player-2 "#2c3e50"}
   :data {:sparql {; Maximum number of results per query
                   :page-size 5000}}
   :guess-similarity-threshold 0.94
   :layout {; Width of hexagon's border (in pixels)
            :border-width 1
            ; Radius of 1 hexagon composing the triangle (in pixels)
            :hex-radius 35
            ; Amount of hexagon's shading (in %)
            :hex-shade 20
            ; Hexagons will be embossed by offset (in %).
            :inner-hex-offset 7
            ; Space between hexagons in % of their size
            :space 8}
   :letters ["A" "B" "C" "Č" "D" "E" "F" "G" "H" "Ch" "I" "J" "K" "L"
             "M" "N" "O" "P" "R" "Ř" "S" "Š" "T" "U" "V" "W" "Z" "Ž"]
   ; Maximum number of characters in a question
   :max-question-length 500
   ; Available time for making a guess (in seconds)
   :time-to-guess 60
   ; Time to display the correct answer (in seconds)
   :verdict-display-time 5})
