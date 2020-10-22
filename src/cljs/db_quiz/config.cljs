(ns db-quiz.config)

; Selector properties
(defonce dcterms-subject "http://purl.org/dc/terms/subject")
(defonce rdf-type "http://www.w3.org/1999/02/22-rdf-syntax-ns#type")

; Namespace prefixes
(def cs-category (partial str "http://cs.dbpedia.org/resource/Kategorie:")) 
(def dbo (partial str "http://dbpedia.org/ontology/"))

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
   :data {:sparql {:endpoint-urls {:cs "https://xtest.vse.cz/virtuoso-dbquiz/sparql"
                                   :en "https://xtest.vse.cz/virtuoso-dbquiz/sparql"}
                   :query-files {:cs {:alphabetic "sparql/cs_dbpedia_az.mustache"
                                      :numeric "sparql/cs_dbpedia.mustache"}
                                 :en {:alphabetic ""
                                      :numeric "sparql/en_dbpedia.mustache"}}}}
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
   :selectors {:artists {:p rdf-type 
                         :o (dbo "Artist")}
               :born-in-brno {:p dcterms-subject
                              :o (cs-category "Narození_v_Brně")}
               :companies {:p rdf-type
                           :o (dbo "Company")} 
               :films {:p rdf-type
                       :o (dbo "Film")}
               :insects {:p rdf-type
                         :o (dbo "Insect")}
               :ksc-members {:p dcterms-subject
                             :o (cs-category "Členové_KSČ")}
               :languages {:p rdf-type
                           :o (dbo "Language")}
               :musicians {:p rdf-type 
                           :o (dbo "MusicalArtist")}
               :persons {:p rdf-type
                         :o (dbo "Person")}
               :places {:p rdf-type
                        :o (dbo "Place")}
               :plants {:p rdf-type
                        :o (dbo "Plant")}
               :politicians {:p rdf-type
                             :o (dbo "Politician")}
               :soccer-players {:p rdf-type
                                :o (dbo "SoccerPlayer")}
               :software {:p rdf-type
                          :o (dbo "Software")}
               :uncertain-death {:p dcterms-subject
                                 :o (cs-category "Osoby_s_nejistým_datem_úmrtí")}
               :works {:p rdf-type
                       :o (dbo "Work")}}
   ; Available time for making a guess (in seconds)
   :time-to-guess 45
   ; Time to display the correct answer (in seconds)
   :verdict-display-time 5})
