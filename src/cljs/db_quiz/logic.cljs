(ns db-quiz.logic
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [db-quiz.state :refer [app-state]]
            [db-quiz.config :refer [config]]
            [db-quiz.normalize :refer [generate-hint normalize-answer]]
            [db-quiz.util :refer [listen number-of-fields redirect toggle]]
            [cljs.core.async :refer [alts! close! chan put! timeout]]
            [clojure.string :as string]
            [clojure.set :refer [intersection union]]
            [clj-fuzzy.jaro-winkler :refer [jaro-winkler]]))

(def ^:private verdict-display-time
  "Time to display the correct answer (in milliseconds)"
  (* (:verdict-display-time config) 1000))

(defn init-board
  "Initialize a data structure representing the game board."
  []
  (let [labels (get-in @app-state [:options :labels])
        size (:board-size config)
        get-sides (fn [x y] (set (remove nil? (list (when (= x 1) :a)
                                                    (when (= x y) :b)
                                                    (when (= y size) :c)))))
        get-neighbours (fn [x y] (set (filter (fn [[x y]] (and ; Conditions for neighbours
                                                               (<= x y)
                                                               (<= 1 x size)
                                                               (<= 1 y size)))
                                              (map (fn [[ox oy]] [(+ x ox) (+ y oy)])
                                                   ; Possible offset of neighbours
                                                   [[-1 -1] [0 -1] [1 0] [0 1] [1 1] [-1 0]]))))
        side (range (inc size))
        symbols-fn (if (and (= labels :alphabetic) (= (count (:letters config)) number-of-fields))
                     (fn [i] (nth (:letters config) (dec i)))
                     identity)]
    (into {}
          (map (fn [[k v] text] [k (assoc v :text text)])
               (mapcat (fn [y] (map (fn [x] [[x y] {:neighbours (get-neighbours x y)
                                                    :ownership :default
                                                    :sides (get-sides x y)}])
                                    (range 1 (inc y))))
                       side)
               ; Get list of symbols of the boxes in the board.
               (map symbols-fn (range 1 (inc number-of-fields)))))))

(defn change-ownership
  [ownership field-id app-state]
  (assoc-in app-state [:board field-id :ownership] ownership))

(def make-active
  (partial change-ownership :active))

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

(defn answer-matches?
  "Test if guess matches the exepcted answer using Jaro-Winkler's string distance.
  Fuzzy matching may be tweaked by setting threshold
  from 0 (everything matches) to 1 (only exact matches)."
  [guess answer & {:keys [threshold]}]
  {:pre [(or (not threshold) (< 0 threshold 1))]}
  (if guess
    (> (jaro-winkler (normalize-answer guess)
                     (normalize-answer answer))
       (or threshold (:guess-similarity-threshold config)))
    false))

(defn clear-answer
  "Clear currently provided answer"
  [app-state]
  (dissoc app-state :answer))

(defn clear-hint
  "Clear currently provided hint"
  [app-state]
  (dissoc app-state :hint))

(defn deselect-current-field
  "Currently selected field is cleared."
  [app-state]
  (dissoc app-state :current-field))

(defn match-answer
  "Mark if the last question was correctly answered or not." 
  [matches? app-state]
  (assoc app-state :verdict matches?))

(defn restart-timer
  [app-state]
  (assoc app-state :timer {:completion 0
                           :start (.getTime (js/Date.))}))

(defn annull-game!
  "Annull abandoned game."
  []
  (swap! app-state (comp #(assoc %
                                 :answer nil
                                 :current-field nil
                                 :timer {:completion 0
                                         :start 0}
                                 :verdict nil))))

(def unmatch-answer
  "Clear the answer match status."
  (partial match-answer nil))

(defn toggle-player
  "Toggling between players"
  [app-state]
  (update app-state :on-turn (partial toggle [:player-1 :player-2])))

(defn test-winner
  "Test if winner exists."
  [owner current-field]
  (let [previous-ownership (get-in @app-state [:board current-field :ownership])
        state (swap! app-state (partial change-ownership owner current-field))
        winner (find-winner (:board state))
        mark-fn (partial match-answer true)]
    (when winner
      (go (swap! app-state (comp mark-fn #(assoc % :winner winner)))
          ; Showing correct answer is not necessary when a missed field is gained.
          (when-not (= previous-ownership :missed) (<! (timeout verdict-display-time)))
          (swap! app-state (comp clear-answer deselect-current-field unmatch-answer))
          (redirect "#end")))))

(defn turn
  [& {:keys [answer answer-matched? correct-answer]}]
  (let [mark-fn (partial match-answer answer-matched?)
        keypresses (listen js/window.document "keydown")]
    (go (swap! app-state mark-fn)
        (alts! [keypresses (timeout verdict-display-time)])
        (close! keypresses)
        (swap! app-state (comp restart-timer toggle-player clear-hint clear-answer
                               deselect-current-field unmatch-answer))))) 

(defn make-a-guess
  []
  (let [{:keys [answer board current-field on-turn verdict]} @app-state
        correct-answer (get-in board [current-field :label])
        answer-matched? (answer-matches? answer correct-answer)
        new-ownership (if answer-matched? on-turn :missed)]
    (when (nil? verdict)
      ; Test if the game is over:
      (test-winner new-ownership current-field)
      (turn :answer answer
            :answer-matched? answer-matched?
            :correct-answer correct-answer))))

(defn pick-field
  "A player picks a field with id on board."
  [id]
  (let [{:keys [board current-field loading? on-turn verdict]} @app-state
        ownership (get-in board [id :ownership])]
    (when (and (nil? current-field) (not loading?) (nil? verdict))
          (case ownership
                :default (swap! app-state (comp (partial make-active id)
                                                restart-timer
                                                (fn [app-state] (assoc app-state :current-field id))))
                :missed (do (test-winner on-turn id)
                            (swap! app-state (comp toggle-player 
                                                   deselect-current-field)))
                nil))))

(defonce timeout-updater
  (js/setInterval (fn []
                    (let [{{:keys [completion start]} :timer
                            :keys [answer board current-field hint verdict]} @app-state
                          time-to-guess (:time-to-guess config)
                          correct-answer (get-in board [current-field :label])]
                      (when (and current-field (nil? verdict))
                        (if (< completion 100)
                          (do (swap! app-state #(assoc-in %
                                                          [:timer :completion]
                                                          (/ (- (.getTime (js/Date.)) start)
                                                              (* 10 time-to-guess))))
                              (when (and (nil? hint) (> completion 50) (nil? answer))
                                (swap! app-state #(assoc % :hint (generate-hint correct-answer)))))
                          (make-a-guess)))))
                  1000))
