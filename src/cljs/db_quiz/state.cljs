(ns db-quiz.state
  (:require [reagent.core :as reagent :refer [atom]]))

(def app-state (atom {:answer nil
                      :board {}
                      :current-field nil
                      :data {:classes :default
                             :difficulty :normal}
                      :loading? false
                      :on-turn (rand-nth [:player-1 :player-2])
                      :players {:player-1 "Dmitrij"
                                :player-2 "Paní M."}
                      :timer {:completion 0
                              :start 0}
                      :verdict nil
                      :winner nil}))
