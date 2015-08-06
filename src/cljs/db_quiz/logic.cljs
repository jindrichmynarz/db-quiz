(ns db-quiz.logic
  (:require [db-quiz.state :as state]
            [clojure.string :as string]
            [clojure.set :refer [intersection union]]
            [clj-fuzzy.jaro-winkler :refer [jaro-winkler]]))

(defn find-connecting-path
  "Finds if there is a continuous path connecting 3 sides of the boards
  through the player-fields starting from the field at coords."
  [player-fields [coords {:keys [neighbours sides]}]]
  {:pre [(map? player-fields)
          (vector? coords)
          (set? neighbours)
          (set? sides)]}
  (let [player-fields-set (set (keys player-fields))]
    (loop [path #{coords}
            visited-sides sides
            visited-fields #{coords}
            fields-to-visit (vec (intersection neighbours player-fields-set))]
      (cond (= visited-sides #{:a :b :c}) path
            (not (seq fields-to-visit)) false
            :else (let [next-field (peek fields-to-visit)
                        {:keys [neighbours sides]} (player-fields next-field)]
                    (recur (conj path next-field)
                            (union visited-sides sides)
                            (conj visited-fields next-field)
                            (into (pop fields-to-visit)
                                  (remove visited-fields
                                          (intersection neighbours player-fields-set)))))))))

(defn find-winner
  "Tries to find a winner based on the current state of the board.
  Returns winner if found, else nil."
  [board]
  (let [get-ownership (comp :ownership second)
        ; Is there a player that owns fields that touch each of the 3 sides?
        has-all-sides? (fn [player-fields]
                         (->> player-fields
                              (map (comp :sides second))
                              (reduce union)
                              (= #{:a :b :c})))
        ; Remove inner fields that are not on any side. These are useless as starting points.
        remove-inner-fields (fn [fields]
                              (filter (comp not empty? :sides second) fields))
        has-sides-connected? (fn [player-fields]
                               (let [player (-> player-fields first get-ownership)
                                     ; TODO: Does first (some) suffice of we need the shortest path?
                                     path (some (partial find-connecting-path (into {} player-fields))
                                                (remove-inner-fields player-fields))]
                                 (when path 
                                   {:player player
                                    :path path})))
        players-fields (->> board
                            (filter (comp #{:player-1 :player-2} get-ownership))
                            (sort-by get-ownership)
                            (partition-by get-ownership))]
    (some has-sides-connected? (filter has-all-sides? players-fields))))

(defn normalize-answer
  [answer]
  (string/lower-case answer))

(defn answer-matches?
  "Test if guess matches the exepcted answer using Jaro-Winkler's string distance.
  Fuzzy matching may be tweaked by setting threshold
  from 0 (everything matches) to 1 (only exact matches)."
  [guess answer & {:keys [threshold]
                   :or {threshold 0.94}}]
  {:pre [(< 0 threshold 1)]}
  (> (jaro-winkler (normalize-answer guess)
                   (normalize-answer answer))
     threshold))

(defn clear-answer
  "Clear currently provided answer"
  [app-state]
  (assoc app-state :answer ""))

(defn deselect-current-field
  "Currently selected field is cleared."
  [app-state]
  (assoc app-state :current-field nil))

(defn toggle
  "Toggle between 2 values given the current value"
  [[one two] value]
  (if (= one value)
    two
    one))

(defn toggle-player
  "Toggling between players"
  [app-state]
  (update app-state :on-turn (partial toggle [:player-1 :player-2])))

(defn turn
  []
  (swap! state/app-state (comp toggle-player clear-answer deselect-current-field)))

(defn pick-field
  "A player picks a field with id on board."
  [board id]
  (let [on-turn (:on-turn @state/app-state)
        ownership (get-in @board [id :ownership])]
    (when (= ownership :default)
      (swap! board #(assoc-in % [id :ownership] :active))
      (swap! state/app-state #(assoc % :current-field id)))))

(defn answer-question
  ""
  [board id correct-answer]
  (let [{:keys [answer on-turn]} @state/app-state
        new-ownership (if (answer-matches? answer correct-answer)
                          on-turn
                          :missed)]
    (swap! board #(assoc-in % [id :ownership] new-ownership))
    ; Test if the game is over:
    (let [board-state @board
          winner (find-winner board-state)]
      (cond ; A player connected all sides of the triangle. 
            (not (nil? winner)) 
            (js/alert "Máme vítěze!")
            ; No more fields in the default or missed ownership.
            (not-any? #{:default :missed} (map :ownership (vals board-state)))
            (js/alert "Nikdo nevyhrál!")
            :else (turn)))))

(defn skip-question
  "A player skips a question. Its field is marked as missed. Player on turn is switched."
  [board id]
  (swap! board #(assoc-in % [id :ownership] :missed))
  (turn))
