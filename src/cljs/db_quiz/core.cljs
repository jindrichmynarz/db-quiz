(ns db-quiz.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [db-quiz.config :refer [config]]
            [db-quiz.model :as model]
            [db-quiz.state :as state]
            [db-quiz.components :as components]
            [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :refer-macros [defroute]]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [cljsjs.react :as react]
            [cljs.core.async :refer [<!]]
            [clojure.string :as string])
  (:import goog.History))

(enable-console-print!)
; (.initializeTouchEvents js/React true)

(defn init-board
  "letters-board is a boolean flag specifying if letters should be used instead of numbers."
  [& {:keys [letters-board]
      :or {letters-board true}}]
  (let [size (:board-size config)
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
        number-of-fields (apply + side)
        symbols-fn (if (and letters-board (= (count (:letters config)) number-of-fields))
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

(defn load-items
  [offset]
  (let [{{:keys [default]} :classes
        :keys [endpoint]} (get-in config [:data :sparql])
        limit (apply + (range (inc (:board-size config))))]
    (go (let [results (map model/despoilerify
                           (<! (model/sparql-query endpoint
                                                   "sparql/cs_dbpedia.mustache"
                                                   :data {:classes default
                                                          :limit limit
                                                          :offset offset})))]
          (reset! state/items results)))))

(defn load-board-data
  [board]
  (let [{:keys [classes endpoint]} (get-in config [:data :sparql])
        limit (apply + (range (inc (:board-size config))))
        data (:data @state/app-state)
        offset (+ (case (:difficulty data)
                        :easy 0
                        :normal 3750
                        :hard 8750)
                  (rand-int 2500))
        chosen-classes ((:classes data) classes)]
    (go (let [results (map model/despoilerify
                           (<! (model/sparql-query endpoint
                                                   "sparql/cs_dbpedia.mustache"
                                                   :data {:classes chosen-classes
                                                          :limit limit
                                                          :offset offset})))]
          (swap! board
                 (fn [board]
                   (into {} (map (fn [[k v] result] [k (merge v result)])
                                 board
                                 results))))))))

(defn load-gdocs-items
  ; Testing data:
  ; spreadsheet-id = 1LbvPMqaKC9pq1PlKK9_WTmNHUBRCSv1JctAeMyLXlzs
  ; worksheet-id = od6
  []
  (letfn [(transform-row [{{label :$t} :gsx$label
                           {description :$t} :gsx$description}]
                            {:label label
                             :abbreviation (model/abbreviate label)
                             :description description})]
    (go (let [results (map transform-row
                           (<! (model/load-gdocs-items "1LbvPMqaKC9pq1PlKK9_WTmNHUBRCSv1JctAeMyLXlzs"
                                                       "od6")))]
          ;(.log js/console (clj->js results))
          (reset! state/gdocs-items results)))))

;; -------------------------
;; Views

(secretary/set-config! :prefix "#")

(defn play-page []
  (let [board (atom (init-board :letters-board false))]
    (load-board-data board)
    (fn []
      [components/play-page board])))

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(defroute "/" []
  (session/put! :current-page components/home-page))

(defroute "/end" []
  (session/put! :current-page components/end-page))

(defroute "/play" []
  (session/put! :current-page play-page))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (hook-browser-navigation!)
  (mount-root))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
