(ns db-quiz.components
  (:require [db-quiz.layout :refer [hex-triangle]]
            [db-quiz.logic :refer [init-board make-a-guess]]
            [db-quiz.model :as model] 
            [db-quiz.state :refer [app-state]]
            [db-quiz.util :refer [join-by-space now redirect toggle]]
            [db-quiz.modals :as modals]
            [db-quiz.geometry :as geo]
            [db-quiz.config :refer [config]]
            [db-quiz.layout :refer [shade-colour]]
            [clojure.string :as string]
            [reagent.core :as reagent :refer [atom]]
            [reagent-forms.core :refer [bind-fields]]
            [reagent-modals.modals :as reagent-modals]))

(defn parse-hash
  [h]
  (let [kv-pairs (-> h
                     (string/replace #"^#\?" "")
                     (string/split #"&"))
        split-fn (fn [kv-pair] (string/split kv-pair #"=" 2))]
    (into {}
          (map (comp (juxt (comp keyword first) second) split-fn)
               kv-pairs))))

(defn set-hash-options!
  []
  (let [location (.-location js/window)
        h (js/decodeURIComponent (.-hash location))]
    (when h
      (when-let [doc-url (:doc (parse-hash h))]
        ; TODO: Refactor
        (swap! app-state (comp #(assoc-in % [:options :doc] (js/decodeURIComponent doc-url))
                               #(assoc-in % [:options :data-source] :gdrive)
                               #(assoc-in % [:options :share-url] (.-href location))))))))

; ----- Common components -----

(defn label-element
  [for-id text]
  [:label.col-sm-4.control-label {:for for-id} text])

(def info-button
  [:button#info-button.btn.btn-default {:on-click #(reagent-modals/modal! modals/game-info
                                                                          {:size :lg})
                                        :title "O hře"}
   [:span.glyphicon.glyphicon-info-sign]])

; ----- Home page -----

(defn loading-indicator
  []
  (when (:loading? @app-state)
    [:div#loading
     [:div#loadhex]
     [:p.vcenter "Načítání..."]]))

(defn player-form-field
  [id label]
  (let [local-id (keyword (str "players." id))]
    [:div {:class (join-by-space "form-group" "player-field" id)}
     [label-element local-id label] 
     [:div.col-sm-8
      [:input.form-control {:field :text :id local-id :max-length 20 :type "text"}]
      [:div.alert.alert-danger
       {:field :alert :id local-id :event empty?}
       "Jméno hráče musí být vyplněno!"]]]))

(def google-spreadsheet-url
  [:div.form-group
   [:p [:label.control-label {:for "options.doc"} "Google Spreadsheet URL"]
         [:a {:on-click #(reagent-modals/modal! modals/google-spreadsheet-help)}
          [:span.glyphicon.glyphicon-question-sign.glyphicon-end]]]
   [:p
    [:input.form-control {:field :text :id :options.doc :type "url"}]]])

(def share-url
  [:div.form-group
   [:p [:label.control-label {:for "options.share-url"} "URL hry"]]
   [:p [:textarea.form-control {:field :text
                                :id :options.share-url
                                :readOnly "readOnly"
                                :rows 4}]]])

(defn select-box
  [single? id label & buttons]
  [:div.form-group
   [label-element id label]
   [:div.col-sm-8.btn-group {:field (if single? :single-select :multi-select)
                              :id (keyword id)
                              :role "group"}
    (for [{:keys [value label]} buttons]
      [:button.btn.btn-default {:key value} label])]])

(def single-select
  (partial select-box true))

(def multi-select
  (partial select-box false))

(def field-labelling
  (single-select "options.labels"
                 "Označování políček"
                {:label "0-9" :value :numeric}
                {:label "A-Z" :value :alphabetic}))

(def language-picker
  (single-select "options.language"
                 "Jazyk"
                 {:label "Česky" :value :czech}
                 {:label "English" :value :english}))

(def difficulty-picker
  (single-select "options.difficulty"
                 "Obtížnost"
                 {:label "Jednoduchá" :value :easy}
                 {:label "Běžná" :value :normal}
                 {:label "Vysoká" :value :hard}))

(def class-picker
  (multi-select "options.classes"
                "Druhy otázek"
                {:label "Osoby" :value "http://dbpedia.org/ontology/Person"}
                {:label "Místa" :value "http://dbpedia.org/ontology/Place"}
                {:label "Díla" :value "http://dbpedia.org/ontology/Work"}))

(def google-spreadsheet-options
  [:div google-spreadsheet-url
        share-url])

(def dbpedia-options
  [:div language-picker
        class-picker
        difficulty-picker
        field-labelling])

(def advanced-options
  (let [toggle-data-source (partial toggle [:dbpedia :gdrive])
        click-handler {:on-click (fn [_] (swap! app-state
                                                #(update-in %
                                                            [:options :data-source]
                                                            toggle-data-source)))}
        tab-pane-class (partial join-by-space "tab-pane")
        generate-share-url (fn [url] 
                             (let [location (.-location js/window)]
                               (str (.-origin location)
                                    (.-pathname location)
                                    "#?doc="
                                    (js/encodeURIComponent url))))]
    (fn []
      (let [[dbpedia-class gdrive-class] (case (get-in @app-state [:options :data-source])
                                               :dbpedia ["active" ""]
                                               :gdrive ["" "active"])]
        [:div
         [:p "Zvolte zdroj otázek:"]
         [:ul.nav.nav-tabs 
          [:li {:class dbpedia-class} [:a click-handler "DBpedia"]]
          [:li {:class gdrive-class} [:a click-handler "Google Spreadsheet"]]]
         [:div.tab-content
          [:div {:class (tab-pane-class dbpedia-class)}
            [bind-fields dbpedia-options app-state]]
          [:div {:class (tab-pane-class gdrive-class)}
            [bind-fields
             google-spreadsheet-options
             app-state
             (fn [id value state]
               (when (= (vec id) [:options :doc])
                 (assoc-in state
                           [:options :share-url]
                           (generate-share-url value))))]]]]))))

(def basic-options
  [:div
   (player-form-field "player-1" "1. hráč")
   (player-form-field "player-2" "2. hráč")])

(def start-menu
  (let [options-hidden (atom true)]
    (set-hash-options!)
    (fn []
      [:div.col-sm-6.col-sm-offset-3
       [:div#start-menu.form-horizontal
        [bind-fields basic-options app-state]
        [:div#advanced 
         [:h4
          [:a {:on-click (fn [e]
                           (swap! options-hidden not)
                           (.preventDefault e))}
           [:span {:class (join-by-space "glyphicon"
                                         "glyphicon-start"
                                         (if @options-hidden
                                           "glyphicon-chevron-right"
                                           "glyphicon-chevron-down"))}]
           "Pokročilé nastavení"]]
         (when-not @options-hidden
           [advanced-options])]
        [:a.button {:on-click #(; TODO: Validate options
                                model/load-board-data (init-board)
                                                      (partial redirect "#play"))}
         [:span "Hrát"]]]])))

(defn home-page
  []
  [:div.container-fluid
   [loading-indicator]
   info-button
   [:div#logo [:img {:alt "DB quiz logo"
                     :src "/img/logo.svg"}]]
   [start-menu]
   [reagent-modals/modal-window]])

; ----- Play page -----

(def autocomplete
  [:input.form-control {:autoFocus "autoFocus"
                        :field :text
                        :id :answer
                        :on-key-down (fn [e]
                                       ; Submit a guess by pressing Enter
                                       (when (= (.-keyCode e) 13)
                                         (make-a-guess)))
                        :placeholder "Odpověď"
                        :spellCheck "false"
                        :type "text"}])

(defn timeout
  [on-turn completion]
  [:div.row
   [:div#timeout {:class (name on-turn)}]
   [:div#timeout-shade {:style {:margin-left (str completion "%")
                                :width (str (- 100 completion) "%")}}]])

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

(defn tick-canvas
  "Animation of a tick symbol."
  []
  (canvas-element "tick-canvas"
                  [20 20]
                  [[[0.2 0.6] [0.4 0.8] [0.8 0.4]]]
                  :line-width 0.2
                  :stroke-colour "#3C763D"))

(defn cross-canvas
  "Cross-mark animated on a canvas"
  []
  (canvas-element "cross-canvas"
                  [15 15]
                  [[[0.1 0.1] [0.9 0.9]]
                   [[0.9 0.1] [0.1 0.9]]]
                  :line-width 0.2
                  :stroke-colour "#A94442"))

(defn verdict-component
  []
  (let [{:keys [board current-field verdict]} @app-state
        correct-answer (get-in board [current-field :label]) 
        {:keys [icon
                success
                verdict-class]} (if verdict
                                  {:icon tick-canvas
                                   :success "Ano"
                                   :verdict-class "alert-success"}
                                  {:icon cross-canvas
                                   :success "Ne"
                                   :verdict-class "alert-danger"})]
    (when-not (nil? verdict)
      [:div#verdict.row
       [:div.col-sm-12
        [:p {:class (str "alert " verdict-class)}
         [icon]
         success ". Správná odpověď je " [:strong correct-answer] "."]]])))

(defn question-box
  [id]
  (let [{:keys [board]} @app-state
        {:keys [abbreviation description label]} (board id)]
    [:div
      [:div.row
        [:div.col-sm-12
          [:h2 abbreviation]
          [:p#description description]]]
      [:div.row
        [:div.col-sm-12
          [:div.input-group
            [bind-fields autocomplete app-state]
            [:span.input-group-btn
             [:button.btn.btn-primary
              {:on-click make-a-guess
               :title "Odpovědět"}
              [:span.glyphicon.glyphicon-ok]
              " Odpovědět"]
             [:button.btn.btn-danger
              {:on-click make-a-guess
               :title "Nevim, dál!"}
              [:span.glyphicon.glyphicon-forward]
              " Dál"]]]]]
      [verdict-component]]))

(defn easter-egg
  []
  (let [[width height] [40 20]]
    (reagent/create-class
      {:component-did-mount (fn [this]
                              (let [context (.getContext (reagent/dom-node this) "2d")
                                    _ (set! (.-fillStyle context)
                                            (shade-colour (get-in config [:colours :player-1]) -30))
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

(defn player-on-turn
  "Show the name of the player, whose turn it currently is."
  []
  (let [{{:keys [completion start]} :timer
         :keys [on-turn players]} @app-state
        player-name (on-turn players)
        player-name-length (count player-name)
        font-class (cond (< player-name-length 6) "font-large"
                         (< player-name-length 10) "font-regular"
                         :else "font-small")]
    [:div
      [:div.row
        [:div#on-turn {:class (str (name on-turn) " " font-class)}
          (if (> player-name-length 20)
            (str (subs player-name 0 17) "...")
            player-name)
          (when (and (= on-turn :player-1) (= player-name "Rybička")) [easter-egg])]]
      [timeout on-turn completion]]))

(defn play-page
  []
  (let [{:keys [current-field]} @app-state]
    [:div.container-fluid
     [:div.row
      [:div.col-sm-6 [hex-triangle]]
      [:div#question-box.col-sm-6
       [player-on-turn]
       (when current-field 
         [question-box current-field])]]]))

; ----- End page -----

(defn end-page
  []
  (let [{{:keys [player] :as winner} :winner
         {:keys [share-url]} :options
         :keys [players]} @app-state
        winner-name (players player)]
    [:div
     (when winner
       [:div#winner
        [:p "Vítězem se stává"]
        [:h1 {:class (name player)} winner-name]])
     [:a.button {:href (or share-url "")} [:span "Hrát znovu"]]]))
