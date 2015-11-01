(ns db-quiz.layout.svg
  (:require [db-quiz.config :refer [config]]
            [db-quiz.logic :as logic]
            [db-quiz.state :refer [app-state]]
            [db-quiz.util :refer [join-by-space shade-colour]]
            [db-quiz.i18n :refer [t]]
            [cljs-http.client :refer [generate-query-string]]
            [clojure.string :as string]))

(defn get-gradients
  "Generate SVG gradients for given status and colour."
  [status colour]
  (let [{{:keys [hex-shade]} :layout} config
        start [:stop {:offset "0%" :stop-color colour}]
        end [:stop {:offset "100%" :stop-color (shade-colour colour hex-shade)}]
        [inner-id outer-id] (map (partial str status) ["-inner" "-outer"])]
    [^{:key inner-id}
     [:linearGradient {:id inner-id :x1 0 :x2 0 :y1 1 :y2 0} start end]
     ^{:key outer-id}
     [:linearGradient {:id outer-id :x1 0 :x2 0 :y1 0 :y2 1} start end]]))

(defn hex-corner
  "Generate coordinates for a hexagon's corner, where
  [x y] are the coordinates of the hexagon's center,
  size is the hexagon's diameter, and i the corner's degree (from 0 to 5)."
  [[x y] size i]
  (let [round (fn [n] (.toFixed n 5))
        angle-deg (+ (* 60 i) 90)
        angle-rad (* (/ (.-PI js/Math) 180) angle-deg)]
    [(round (+ x (* (/ size 2) (.cos js/Math angle-rad))))
     (round (+ y (* (/ size 2) (.sin js/Math angle-rad))))]))

(defn hex-coords
  "Generates coordinates for a hexagon of size
  centered at center [x y]."
  [center size]
  (string/join " "
               (map (comp (partial string/join ",")
                          (partial hex-corner center size))
                    (range 6))))

(defn hexagon
  "Generate hexagon of size containing text
  centered at center [x y]."
  [{:keys [center id size text]}]
  (let [absolute-offset (* (/ size 100) (get-in config [:layout :inner-hex-offset]))
        [x y] center]
    (fn [{:keys [center id size text]}]
      (let [{:keys [board current-field loading?]} @app-state
            {:keys [abbreviation deselected? ownership]} (board id)
            disabled? (not (nil? current-field))
            availability (if (or loading? disabled? (not (#{:default :missed} ownership)))
                             "unavailable"
                             (case ownership
                                   :default "available"
                                   :missed "missed"))
            ownership-name (name ownership)
            {:keys [font-size-ratio label]} (if (= ownership :active)
                                                 {:font-size-ratio (max (dec (count abbreviation)) 3)
                                                  :label abbreviation}
                                                 {:font-size-ratio 2
                                                  :label text})]
        [:g {:class (join-by-space "hexagon"
                                   availability
                                   (when (= ownership :active) "active")
                                   (when deselected? "deselected"))
             :on-click (partial logic/pick-field id)}
         [:polygon.hex-outer {:fill (str "url(#" ownership-name "-outer)")
                              :points (hex-coords center size)}]
         [:polygon.hex-inner {:fill (str "url(#" ownership-name "-inner)")
                              :points (hex-coords (map #(- % absolute-offset) center)
                                                  (- size (* 2 absolute-offset)))
                              :transform (str "translate(" absolute-offset "," absolute-offset ")")}]
         [:text {:x x
                 :y (+ y (/ size (* 2.5 font-size-ratio)))
                 :font-size (/ size font-size-ratio)
                 :text-anchor "middle"}
          label]]))))

(def hex-triangle
  "Component that generates triangular board of hexagons."
  (let [{{:keys [border-width space]
          r :hex-radius} :layout
         n :board-size} config
        size (* 2 r)
        padding r
        y-space (* size (/ space 100))
        x-space (* y-space (/ (.sqrt js/Math 3) 2))
        w (* (.sqrt js/Math 3) r)
        grid-width (+ (* n w)
                      (* 2 border-width) ; Account for hexagon's border
                      (* (dec n) x-space)
                      padding)
        grid-height (+ (* (/ 3 2) r n) (/ r 2)
                       (* (dec n) y-space) ; Add height for spaces
                       (* 2 border-width) ; Account for hexagon's border
                       padding)
        x-offset (fn [x y] (+ (* (- n (inc y)) (/ w 2))
                              (* x w)
                              (when-not (= x 1) (* (dec x) x-space)) ; Account for spaces
                              (when-not (= y n) (* (- n y) (/ x-space 2)))
                              border-width ; Account for hexagon's border
                              (/ padding 2)))
        y-offset (fn [y] (+ r
                            border-width ; Account for hexagon's border
                            (* (/ 3 2) r (dec y))
                            (when-not (= y 1) (* (dec y) y-space)) ; Account for spaces
                            (/ padding 2)))]
    (fn []
      [:svg#hex-triangle {:x 0 :y 0
                          :width grid-width
                          :height grid-height}
       [:defs (mapcat (fn [[status colour]]
                        (get-gradients (name status) colour))
                      (:colours config))]
       (map (fn [[[x y] options]]
              ^{:key [x y]}
              [hexagon (assoc options
                              :center [(x-offset x y)
                                       (y-offset y)]
                              :id [x y]
                              :size size)])
            (sort-by (comp #(select-keys % [:deselected? :ownership]) second)
                     (fn [{:keys [deselected? ownership]} b]
                       (if (or (= ownership :active) deselected?) 1 -1))
                     (:board @app-state)))])))

(defn winners-cup
  "Winner's cup coloured with the colour of the winning player."
  [colour]
  [:svg {:width 250 :height 250}
    [:defs
     [:linearGradient
       {:gradientUnits "userSpaceOnUse"
        :id "cup-gradient"
        :x1 0 :y1 0
        :x2 177.79153 :y2 96.346634}
       [:stop {:offset 0
               :style {:stop-color colour
                       :stop-opacity 0}}]
       [:stop {:offset 1
               :style {:stop-color colour
                       :stop-opacity 1}}]]]
   [:g
    [:path {:d "m 23.094464,-2e-5 23.522065,57.01846 -12.801831,22.16903 45.725784,79.20482 8.89883,0 9.488616,23.019 15.351792,0 0,35.95961 0.0694,0 -33.773938,16.3058 0.50306,16.3233 90.688298,0 0.15613,-16.3233 -33.75659,-16.3058 0.0693,0 0,-35.95961 15.21301,0 9.50598,-23.019 9.03761,0 L 216.70041,79.18747 203.88122,56.9664 227.42063,-2e-5 l -56.42865,0 -91.451568,0 -56.446015,0 z m 175.981796,68.58867 6.12336,10.59882 -36.9657,64.04383 30.84234,-74.64265 z m -147.654708,0.0694 30.44338,73.81003 -36.532055,-63.28059 6.088675,-10.52944 z"
            :id "cup"
            :style {:fill "url(#cup-gradient)"}}]
    [:path {:d "M 34.526818,-51.55751 52.996969,21.61337 -3.3828659,-29.67722 47.907724,26.68531 -25.263158,8.21516 47.336482,28.79717 -25.263158,51.97574 49.223311,35.73862 -3.3828669,89.86811 50.746623,37.26193 34.526818,111.7484 57.688076,39.14876 78.270092,111.7484 59.79994,38.56021 116.17977,89.86811 64.889185,33.48828 138.04275,51.97574 65.443117,31.39373 138.04275,8.21516 63.573598,24.43496 116.17977,-29.67722 62.050286,22.91165 78.270092,-51.55751 55.091523,21.04213 34.526818,-51.55751 z"
            :id "shine"
            :style {:fill "#fff"}}]]])

(defn not-found
  "Not found hexagon"
  []
  [:svg {:id "not-found"
         :xmlns "http://www.w3.org/2000/svg"
         :width 300
         :height 310}
    [:defs
     [:linearGradient
       {:gradientUnits "userSpaceOnUse"
        :id "not-found-gradient"
        :x1 0 :y1 0
        :x2 300 :y2 300}
       [:stop {:offset 0
               :style {:stop-color "#fc4349"
                       :stop-opacity 1}}]
       [:stop {:offset 1
               :style {:stop-color "#354d65"
                       :stop-opacity 1}}]]]
    [:path {:d "M150 0L20.094 75v150L150 300l129.906-75V75L150 0zm-.094 108.906c9.215 0 16.146 3.476 20.844 10.47 4.735 6.992 7.125 17.242 7.125 30.718 0 13.95-2.317 24.267-6.906 30.97-4.554 6.7-11.558 10.06-21.064 10.06-9.214 0-16.177-3.485-20.875-10.405-4.66-6.92-7-17.114-7-30.626 0-14.095 2.26-24.492 6.814-31.156 4.552-6.702 11.593-10.032 21.062-10.032zm-58.812 1.25h15.594v50.188h9.593v13.125h-9.593v16.56h-16.5v-16.56H56.094v-11.75l35-51.564zm127.625 0h15.56v50.188h9.626v13.125h-9.625v16.56h-16.5v-16.56h-34.06v-11.75l35-51.564zm-69.564 12.406c-3.59.252-6.176 2.41-7.78 6.438-1.713 4.26-2.564 11.297-2.564 21.094s.825 16.8 2.5 21.062c1.712 4.225 4.588 6.344 8.594 6.344 3.934 0 6.752-2.14 8.5-6.438 1.748-4.297 2.625-11.28 2.625-20.968 0-9.797-.902-16.833-2.686-21.094-1.748-4.298-4.54-6.437-8.438-6.438-.248 0-.51-.016-.75 0zm-58.72 6.938c-1.346 2.987-2.948 5.878-4.842 8.72l-14.656 22.124h19.25v-13.53c0-2.26.067-5.548.25-9.845.182-4.3.328-6.778.437-7.47h-.438zm127.626 0c-1.347 2.987-2.95 5.878-4.843 8.72l-14.657 22.124h19.218v-13.53c0-2.26.1-5.548.282-9.845.183-4.3.33-6.778.438-7.47h-.438z"
            :style {:fill "url(#not-found-gradient)"
                    :filter "url(#drop-shadow)"}}]])
