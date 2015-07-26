(ns db-quiz.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [db-quiz.layout :refer [hex-triangle]]
            [db-quiz.config :refer [config]]
            [db-quiz.model :as model]
            [db-quiz.state :as state] 
            [reagent.core :as reagent :refer [atom]]
            [reagent-forms.core :refer [bind-fields]]
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
        get-neighbours (fn [x y] (filter (fn [[x y]] (and ; Conditions for neighbours
                                                          (<= x y)
                                                          (<= 1 x size)
                                                          (<= 1 y size)))
                                         (map (fn [[ox oy]] [(+ x ox) (+ y oy)])
                                              ; Possible offset of neighbours
                                              [[-1 -1] [0 -1] [1 0] [0 1] [1 1] [-1 0]])))
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

(defn autocomplete-did-mount []
  (js/$ (fn []
          (.autocomplete (js/$ "#guess") 
                         (clj->js {:delay 1000
                                   :minLength 3
                                   :source model/wikipedia-autocomplete})))))

; ----- Templates -----

(defn label-element
  [for-id text]
  [:label.col-sm-2.control-label {:for for-id} text])

(defn difficulty-slider
  []
  [:p
   [:strong [:label {:for "offset"} "Obtížnost: "]]
   [:input {:type "range" :id "offset" :min 0 :max 10000 
            :on-mouse-up (fn [e] (let [offset (-> e .-target .-value int)]
                                   (load-items (+ offset (rand-int (- 1000 28))))))}]])

(defn loading-indicator
  []
  (when @model/loading?
    [:div#loading
     [:div#loadhex ""]
     [:p.vcenter "Načítání..."]]))

(defn player-on-turn
  []
  (let [{:keys [on-turn players]} @state/app-state
        player-name (on-turn players)
        player-name-length (count player-name)
        font-class (cond (< player-name-length 6) "font-large"
                         (< player-name-length 10) "font-regular"
                         :else "font-small")]
    [:div#on-turn {:class (name on-turn)}
     [:div {:class (str font-class " vcenter")}
      (if (> player-name-length 20)
        (str (subs player-name 0 17) "...")
        player-name)]]))

(defn items-table
  [items]
  [:table
   [:thead [:tr [:th "Odpověď"] [:th "Zkratka"] [:th "Popis"]]]
   [:tbody (map (fn [{:keys [abbreviation description label url]}]
                  ^{:key label} [:tr [:td (if url
                                            [:a {:href url} label]
                                            label)]
                                 [:td abbreviation]
                                 [:td description]])
                items)]])

(defn autocomplete []
  [:div.ui-widget
    (label-element "guess" "Odpověď")
    [:div.col-sm-4 [:input#guess.form-control {:field :text :type "text"}]]])

(defn autocomplete-component []
  (reagent/create-class {:reagent-render autocomplete
                         :component-did-mount autocomplete-did-mount}))

(defn player-form-field
  [id label]
  (let [local-id (keyword (str "players." id))]
    [:div.form-group [label-element local-id label] 
                     [:div.col-sm-10 [:input.form-control {:field :text :id local-id :type "text"}]]]))

(def difficulty-picker
  [:div.form-group
   [label-element "data.difficulty" "Obtížnost"]
   [:div.btn-group.col-sm-10 {:field :single-select :id :data.difficulty :role "group"}
    [:button.btn.btn-default {:key :easy} "Jednoduchá"]
    [:button.btn.btn-default {:key :normal} "Běžná"]
    [:button.btn.btn-default {:key :hard} "Vysoká"]]])

(def start-menu
  [:div#start-menu.form-horizontal
    (player-form-field "player-1" "1. hráč")
    (player-form-field "player-2" "2. hráč")
    difficulty-picker
    [:a.button {:href "/#play"} [:span "Hrát"]]])

(defn question-box
  [{:keys [abbreviation description label]}]
  [:div#question-box.col-sm-12
    [:h2 abbreviation]
    [:p#description description]
    [autocomplete-component]
    [:p label]])

;; -------------------------
;; Views

(secretary/set-config! :prefix "#")

(defn home-page []
  [:div.container-fluid
   [:div#logo [:img {:alt "DB quiz logo"
                     :src "/img/logo.svg"}]]
   [:div.col-sm-6.col-md-offset-3
    [bind-fields start-menu state/app-state]]])

(defn play-page []
  (let [board (atom (init-board :letters-board false))]
    (load-board-data board)
    (fn []
      [:div.container-fluid
       [player-on-turn]
       [loading-indicator]
       [:div.col-sm-12 [hex-triangle board]]
       (when-let [current-field (@board (:current-field @state/app-state))]
         [question-box current-field])])))

(defn end-page []
  [:div
    [:h2 "Všechno jednou končí..."]
    [:p "Hamižnost opět zvítězila nad pravdomluvností."]
    [:a.button {:href "/"} [:span "Hrát znovu"]]])

(defn items-page []
  (load-items 5000) 
  (fn []
    [:div
     [loading-indicator] 
     [difficulty-slider]
     [items-table @state/items]]))

(defn gdocs-items-page []
  (load-gdocs-items)
  (fn []
    [:div
     [loading-indicator]
     [items-table @state/gdocs-items]]))

(defn autocomplete-page []
  (let [doc (atom {})]
    (fn []
      [:div
       [:h1 "Autocomplete"]
       [autocomplete-component]])))

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(defroute "/" []
  (session/put! :current-page #'home-page))

(defroute "/end" []
  (session/put! :current-page #'end-page))

(defroute "/play" []
  (session/put! :current-page #'play-page))

(defroute "/items" []
  (session/put! :current-page #'items-page))

(defroute "/gdocs-items" []
  (session/put! :current-page #'gdocs-items-page))

(defroute "/autocomplete" []
  (session/put! :current-page #'autocomplete-page))

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
