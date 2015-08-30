(ns db-quiz.layout.canvas
   (:require [db-quiz.config :refer [config]]
             [db-quiz.geometry :as geo]
             [db-quiz.util :refer [now shade-colour]]
             [reagent.core :as reagent]))

(defn canvas-renderer
  "Path is made of segments, i.e. [segment-1 segment-2].
  Each segment is a vector of coordinates, i.e. [[x1 y1] [x2 y2]]."
  [this [width height] relative-path & {:keys [animation-duration line-width stroke-colour]}]
  (let [context (.getContext (reagent/dom-node this) "2d")
        _ (set! (.-lineWidth context) (* width line-width))
        _ (set! (.-strokeStyle context) stroke-colour)
        path (geo/relative-to-absolute-coords [width height] relative-path)
        total-distance (geo/path-length path)
        step-length (/ total-distance animation-duration)
        start-time (now)]
    (fn render []
      (.clearRect context 0 0 width height)
      (let [distance-travelled (min (* step-length (- (now) start-time))
                                    total-distance)
            segments (geo/path-travelled path distance-travelled)]
        (.beginPath context)
        (doseq [[[start-x start-y] & tail] segments]
          (.moveTo context start-x start-y)
          (doseq [[x y] tail]
            (.lineTo context x y))
          (.stroke context)))
      (reagent/next-tick render))))

(defn canvas-element
  "Create canvas element identified with id, or given width and height,
  by drawing path in anitmation.
  animation-duration is in seconds.
  line-width is relative line width to width."
  [id [width height] path & {:keys [animation-duration line-width stroke-colour]
                             :or {animation-duration 0.5
                                  line-width 0.1
                                  stroke-colour "#000000"}}]
  (reagent/create-class
      {:component-did-mount (fn [this]
                              (reagent/next-tick (canvas-renderer this
                                                                  [width height]
                                                                  path
                                                                  :animation-duration animation-duration
                                                                  :line-width line-width
                                                                  :stroke-colour stroke-colour)))
       :display-name id
       :reagent-render (fn [] [:canvas {:width width :height height}])}))

(defn cross
  "Cross-mark animated on a canvas"
  []
  (canvas-element "cross-canvas"
                  [15 15]
                  [[[0.1 0.1] [0.9 0.9]]
                   [[0.9 0.1] [0.1 0.9]]]
                  :line-width 0.2
                  :stroke-colour "#A94442"))

(defn easter-egg
  "Easter egg that renders a symbol of fish."
  [player]
  (let [[width height] [40 20]]
    (reagent/create-class
      {:component-did-mount (fn [this]
                              (let [context (.getContext (reagent/dom-node this) "2d")
                                    _ (set! (.-fillStyle context)
                                            (shade-colour (get-in config [:colours player]) -30))
                                    relative-path [[[0 0.5] [0.2 0] [0.55 0] [0.8 0.45]]
                                                   [[0.8 0.45] [1 0.2] [0.8 0.45] [1 0.2]]
                                                   [[1 0.2] [0.93 0.5] [0.93 0.5] [1 0.8]]
                                                   [[1 0.8] [0.8 0.55] [1 0.8] [0.8 0.55]]
                                                   [[0.8 0.55] [0.55 1] [0.2 1] [0 0.5]]]
                                    path (geo/relative-to-absolute-coords [width height]
                                                                          relative-path)
                                    [start-x start-y] (ffirst path)]
                                (doto context
                                  (.beginPath)
                                  (.moveTo start-x start-y))
                                (doseq [[_ [cp1x cp1y] [cp2x cp2y] [x y]] path]
                                  (.bezierCurveTo context
                                                  cp1x cp1y
                                                  cp2x cp2y
                                                  x y))
                                (.fill context)))
       :display-name "easter-egg"
       :reagent-render (fn [] [:canvas#easter-egg {:width width :height height}])})))

(defn tick
  "Animation of a tick symbol."
  []
  (canvas-element "tick-canvas"
                  [20 20]
                  [[[0.2 0.6] [0.4 0.8] [0.8 0.4]]]
                  :line-width 0.15
                  :stroke-colour "#3C763D"))
