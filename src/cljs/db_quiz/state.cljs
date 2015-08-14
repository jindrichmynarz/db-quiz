(ns db-quiz.state
  (:require [reagent.core :as reagent :refer [atom]]))

(defonce app-state
  (atom {:answer nil
         :board {}
         :current-field nil
         :loading? false
         :on-turn (rand-nth [:player-1 :player-2])
         :options {:classes ["http://dbpedia.org/ontology/Person"
                             "http://dbpedia.org/ontology/Place"
                             "http://dbpedia.org/ontology/Work"]
                   :data-source :dbpedia
                   :difficulty :normal
                   :doc ""
                   :labels :numeric
                   :language :czech
                   :share-url ""}
         :players {:player-1 "Dmitrij"
                   :player-2 "Paní M."}
         :timer {:completion 0
                 :start 0}
         :verdict nil
         :winner nil}))
