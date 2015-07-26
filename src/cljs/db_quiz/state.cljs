(ns db-quiz.state
  (:require [reagent.core :as reagent :refer [atom]]))

(def app-state (atom {:current-field nil
                      :data {:classes :default
                             :difficulty :normal}
                      :on-turn (rand-nth [:player-1 :player-2])
                      :players {:player-1 "Dmitrij"
                                :player-2 "Paní M."}}))

(def items (atom []))

(def gdocs-items (atom []))
