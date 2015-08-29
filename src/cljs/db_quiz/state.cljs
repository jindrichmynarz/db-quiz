(ns db-quiz.state
  (:require [reagent.core :as reagent :refer [atom]]))

(defonce app-state
  (atom {:answer nil
         :board {}
         :current-field nil
         :hint nil
         :loading? false
         :on-turn (rand-nth [:player-1 :player-2])
         :options {:data-source :dbpedia
                   :difficulty :normal
                   :doc ""
                   :labels :numeric
                   :language :czech
                   :selectors [{:p "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
                                :o "http://dbpedia.org/ontology/Person"}
                               {:p "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
                                :o "http://dbpedia.org/ontology/Place"}
                               {:p "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
                                :o "http://dbpedia.org/ontology/Work"}]
                   :share-url ""}
         :players {:player-1 "Dmitrij"
                   :player-2 "Paní M."}
         :timer {:completion 0
                 :start 0}
         :verdict nil
         :winner nil}))
