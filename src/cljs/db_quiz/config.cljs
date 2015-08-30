(ns db-quiz.config)

(defonce config
  {; Number of hexagons on one side of the triangle
   :board-size 7
   ; Colours of boards fields in various states
   :colours {:active "#eeeeee"
             :default "#bcbcbc"
             :hover "#dedede"
             :hover-missed "#4d4d4d"
             :missed "#333333"
             :player-1 "#fc4349"
             :player-2 "#354d65"}
   :data {:sparql {; Maximum number of results per query
                   :page-size 5000}}
   ; Similarity threshold required for the guess to match the correct answer
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
   ; Letters used to label fields in AZ-kvíz. Notice there is no "Y", for example.
   :letters ["A" "B" "C" "Č" "D" "E" "F" "G" "H" "Ch" "I" "J" "K" "L"
             "M" "N" "O" "P" "R" "Ř" "S" "Š" "T" "U" "V" "W" "Z" "Ž"]
   ; Maximum number of characters in a question
   :max-question-length 500
   ; Selectors for filtering questions retrieved by SPARQL queries
   :selectors {:persons {:p "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
                         :o "http://dbpedia.org/ontology/Person"}
               :places {:p "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
                        :o "http://dbpedia.org/ontology/Place"}
               :works {:p "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
                       :o "http://dbpedia.org/ontology/Work"}
               :born-in-brno {:p "http://purl.org/dc/terms/subject"
                              :o "http://cs.dbpedia.org/resource/Kategorie:Narození_v_Brně"}
               :ksc-members {:p "http://purl.org/dc/terms/subject"
                             :o "http://cs.dbpedia.org/resource/Kategorie:Členové_KSČ"}
               :uncertain-death {:p "http://purl.org/dc/terms/subject"
                                 :o "http://cs.dbpedia.org/resource/Kategorie:Osoby_s_nejistým_datem_úmrtí"}
               :artists {:p "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
                         :o "http://dbpedia.org/ontology/Artist"}
               :politicians {:p "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
                             :o "http://dbpedia.org/ontology/Politician"}
               :musicians {:p "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
                           :o "http://dbpedia.org/ontology/MusicalArtist"}
               :films {:p "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
                       :o "http://dbpedia.org/ontology/Film"}}
   ; Available time for making a guess (in seconds)
   :time-to-guess 45
   ; Time to display the correct answer (in seconds)
   :verdict-display-time 5})
