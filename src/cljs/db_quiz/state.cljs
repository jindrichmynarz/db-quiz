(ns db-quiz.state
  (:require [reagent.core :as reagent :refer [atom]]))

(defonce app-state
  (atom {:answer nil
         :answers {:correct 0
                   :incorrect 0}
         :board {}
         :current-field nil
         :hint nil
         :language :cs
         :loading? false
         :on-turn (rand-nth [:player-1 :player-2])
         :options {:data-source :dbpedia
                   :despoilerify? true
                   :difficulty :normal
                   :doc ""
                   :labels :numeric
                   :selectors #{:persons
                                :places
                                :works}
                   :share-url ""}
         :players {:player-1 "Dmitrij"
                   :player-2 "Paní M."}
         :timer {:completion 0
                 :start 0}
         :verdict nil
         :winner nil}))
