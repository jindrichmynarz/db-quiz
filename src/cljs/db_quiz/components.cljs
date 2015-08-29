(ns db-quiz.components
  (:require [db-quiz.layout :refer [hex-triangle shade-colour]]
            [db-quiz.logic :refer [annull-game! init-board make-a-guess]]
            [db-quiz.model :as model] 
            [db-quiz.state :refer [app-state]]
            [db-quiz.util :refer [join-by-space now redirect toggle]]
            [db-quiz.modals :as modals]
            [db-quiz.geometry :as geo]
            [db-quiz.config :refer [config]]
            [db-quiz.normalize :refer [trim-player-name]]
            [clojure.string :as string]
            [reagent.core :as reagent :refer [atom]]
            [reagent-forms.core :refer [bind-fields]]
            [reagent-modals.modals :as reagent-modals]))

(defn check-if-online
  "If browser is offline, show a warning modal.
  Warning: This is unreliable, since browsers implement it inconsistently."
  []
  (when-not js/navigator.onLine (reagent-modals/modal! modals/offline)))

(defn parse-hash
  "Parse 'query parameters' from hash fragment in the URL.
  For example '#?doc=1234' is parsed to {:doc \"1234\"}."
  [h]
  (let [kv-pairs (-> h
                     (string/replace #"^#\?" "")
                     (string/split #"&"))
        split-fn (fn [kv-pair] (string/split kv-pair #"=" 2))]
    (into {}
          (map (comp (juxt (comp keyword first) second) split-fn)
               kv-pairs))))

(defn set-hash-options!
  "Set options based on the params in URL's hash fragment.
  Used to set Google Spreadsheet URL."
  []
  (let [location (.-location js/window)
        h (js/decodeURIComponent (.-hash location))]
    (when h
      (when-let [doc-url (:doc (parse-hash h))]
        (swap! app-state (comp #(assoc-in % [:options :doc] (js/decodeURIComponent doc-url))
                               #(assoc-in % [:options :data-source] :gdrive)
                               #(assoc-in % [:options :share-url] (.-href location))))))))

(defn set-language!
  "Set preferred language based on the browser's settings.
  Only supports Czech and English with Czech as default."
  []
  (let [language (case (.slice (.-language js/navigator) 0 2)
                       "en" :english
                       "cs" :czech
                       :czech)]
    (swap! app-state #(assoc-in % [:options :language] language))))

(defn set-defaults!
  "Set default options on load."
  []
  (check-if-online)
  (set-hash-options!))

(defn validate-options
  []
  (let [{{:keys [data-source difficulty doc
                 labels selectors]} :options
         {:keys [player-1 player-2]} :players} @app-state
        name-errors [(when (empty? player-1) "Chybí jméno hráče 1.")
                     (when (empty? player-2) "Chybí jméno hráče 2.")]
        data-errors (case data-source
                      :dbpedia [(when-not (pos? (count selectors)) "Alespoň 1 druh otázek musí být zvolen.")
                                (when-not difficulty "Musí být zvolena obtížnost.")
                                (when-not labels "Musí být zvolen způsob označování políček.")]
                      :gdrive [(when (empty? doc) "URL Google Spreadsheetu nesmí být prázdné.")])
        errors (concat name-errors data-errors)]
    {:valid? (every? nil? errors)
     :errors (remove nil? errors)}))

; ----- Common components -----

(def menu
  [:div#info-menu.btn-group {:role "group"}
   [:a.btn.btn-default {:href "/"}
    [:span.glyphicon.glyphicon-home.glyphicon-start]
    "Domů"]
   [:button.btn.btn-default {:on-click #(reagent-modals/modal! modals/game-info
                                                                           {:size :lg})}
    [:span.glyphicon.glyphicon-info-sign.glyphicon-start]
    "O hře"]])

(defn label-element
  "Label with text for input identified with for-id"
  [for-id text]
  [:label.col-sm-4.control-label {:for for-id} text])

(defn select-box
  "Form select box.
  If single? is true then only 1 value can be selected at a time.
  Otherwise, multiple values can be selected.
  id is a string identifier of the select's input.
  label is used to label the input.
  buttons are maps of [:label :value] pairs for the buttons in the select box.
  Less than 3 buttons are presented in a row. More than 3 buttons are presented
  vertically."
  [single? id label & buttons]
  {:pre [(= (type single?) js/Boolean)
         (= (type id) js/String)]}
  (let [btn-group-class (if (> (count buttons) 3)
                          "btn-group-vertical"
                          "btn-group")]
    [:div.form-group
     [label-element id label]
     [:div {:class (join-by-space "col-sm-8" btn-group-class)
            :field (if single? :single-select :multi-select)
            :id (keyword id)
            :role "group"}
      (for [{:keys [value label]} buttons]
        [:button.btn.btn-default {:key value} label])]]))

(def single-select
  (partial select-box true))

(def multi-select
  (partial select-box false))

; ----- Home page -----

(defn loading-indicator
  "Indicates if data is loaded."
  []
  (when (:loading? @app-state)
    [:div#loading
     [:div#loadhex]
     [:p.vcenter "Načítání..."]]))

(defn player-form-field
  "Form field for player's name"
  [id label]
  (let [local-id (keyword (str "players." id))]
    [:div {:class (join-by-space "form-group" "player-field" id)}
     [label-element local-id label] 
     [:div.col-sm-8
      [:input.form-control {:field :text
                            :id local-id
                            :max-length 20
                            :placeholder "Jméno hráče"
                            :type "text"}]]]))

(def google-spreadsheet-url
  "Input for URL of a Google Spreadsheet to load data from"
  [:div.form-group
   [:p [:label.control-label {:for "options.doc"} "Google Spreadsheet URL"]
         [:a {:on-click #(reagent-modals/modal! modals/google-spreadsheet-help)}
          [:span.glyphicon.glyphicon-question-sign.glyphicon-end]]]
   [:p
    [:input.form-control {:field :text :id :options.doc :type "url"}]]])

(def share-url
  "URL of the game based on given Google Spreadsheet"
  [:div.form-group
   [:p [:label.control-label {:for "options.share-url"} "URL hry"]]
   [:p [:textarea.form-control {:field :text
                                :id :options.share-url
                                :readOnly "readOnly"
                                :rows 4}]]])

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
  (multi-select "options.selectors"
                "Druhy otázek"
                {:label "Osoby"
                 :value {:p "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
                         :o "http://dbpedia.org/ontology/Person"}}
                {:label "Místa"
                 :value {:p "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
                         :o "http://dbpedia.org/ontology/Place"}}
                {:label "Díla"
                 :value {:p "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
                         :o "http://dbpedia.org/ontology/Work"}}
                {:label "Narození v Brně"
                 :value {:p "http://purl.org/dc/terms/subject"
                         :o "http://cs.dbpedia.org/resource/Kategorie:Narození_v_Brně"}}
                {:label "Členové KSČ"
                 :value {:p "http://purl.org/dc/terms/subject"
                         :o "http://cs.dbpedia.org/resource/Kategorie:Členové_KSČ"}}
                {:label "Osoby s nejistým datem úmrtí"
                 :value {:p "http://purl.org/dc/terms/subject"
                         :o "http://cs.dbpedia.org/resource/Kategorie:Osoby_s_nejistým_datem_úmrtí"}}
                {:label "Umělci"
                 :value {:p "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
                         :o "http://dbpedia.org/ontology/Artist"}}
                {:label "Politici"
                 :value {:p "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
                         :o "http://dbpedia.org/ontology/Politician"}}
                {:label "Hudebníci"
                 :value {:p "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
                         :o "http://dbpedia.org/ontology/MusicalArtist"}}
                {:label "Filmy"
                 :value {:p "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
                         :o "http://dbpedia.org/ontology/Film"}}))

(def google-spreadsheet-options
  [:div google-spreadsheet-url
        share-url])

(def dbpedia-options
  [:div class-picker
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
    (set-defaults!)
    (fn []
      (annull-game!)
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
        [:a.button {:on-click (fn [e]
                                (let [{:keys [valid? errors]} (validate-options)]
                                  (if valid?
                                    (model/load-board-data (init-board)
                                                           (partial redirect "#play"))
                                    (reagent-modals/modal! (modals/invalid-options errors)))))}
         [:span "Hrát"]]]])))

(defn home-page
  []
  [:div.container-fluid
   [loading-indicator]
   menu
   [:div#logo [:img {:alt "DB quiz logo"
                     :src "/img/logo.svg"}]]
   [start-menu]
   [reagent-modals/modal-window]])

; ----- Play page -----

(defn guess
  []
  (let [{:keys [hint]} @app-state]
    [:input.form-control {:autoFocus "autoFocus"
                          :id :answer
                          :on-change (fn [e] (swap! app-state #(assoc % :answer (.-value (.-target e)))))
                          :on-key-down (fn [e]
                                         ; Submit a guess by pressing Enter
                                         (when (= (.-keyCode e) 13)
                                           (make-a-guess)))
                          :placeholder (or hint "Odpověď") 
                          :spellCheck "false"
                          :type "text"}]))

(defn timeout
  "Progress bar showing the ellapsed time from player's turn."
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
                  :line-width 0.15
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
  "Show verdict if answer was correct or not."
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
        [:p {:class (join-by-space "alert" verdict-class)}
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
            [guess]
            ;[bind-fields guess app-state]
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

(defn player-on-turn
  "Show the name of the player, whose turn it currently is."
  []
  (let [{{:keys [completion start]} :timer
         :keys [on-turn players]} @app-state
        player-name (on-turn players)
        [trimmed-name font-class] (trim-player-name player-name)]
    [:div
      [:div.row
        [:div#on-turn {:class (join-by-space (name on-turn) font-class)}
          trimmed-name
          (when (= player-name "Rybička") [easter-egg on-turn])]]
      [timeout on-turn completion]]))

(defn play-page
  []
  (let [{:keys [board current-field]} @app-state]
    (if (empty? board)
      (redirect "/") ; If no data is loaded, redirect to home page. TODO: Can we redirect to root?
      [:div.container-fluid
       menu
       [:div.row
        [:div.col-sm-6 [hex-triangle]]
        [:div#question-box.col-sm-6
         [player-on-turn]
         (when current-field
           [question-box current-field])]]
       [reagent-modals/modal-window]]))) 

; ----- End page -----

(defn winners-cup
  "Winner's cup coloured with the colour of the winning player."
  [colour]
  [:svg {:width 250 :height 250}
    [:defs
     [:linearGradient
       {:id "cup-gradient"
        :x1 0 :y1 0
        :x2 177.79153 :y2 96.346634
        :gradientUnits "userSpaceOnUse"}
       [:stop {:offset 0
              :style {:stop-color colour
                      :stop-opacity 0}}]
       [:stop {:offset 1
              :style {:stop-color colour
                      :stop-opacity 1}}]]]
   [:g 
    [:path {:style {:fill "url(#cup-gradient)"}
            :d "m 23.094464,-2e-5 23.522065,57.01846 -12.801831,22.16903 45.725784,79.20482 8.89883,0 9.488616,23.019 15.351792,0 0,35.95961 0.0694,0 -33.773938,16.3058 0.50306,16.3233 90.688298,0 0.15613,-16.3233 -33.75659,-16.3058 0.0693,0 0,-35.95961 15.21301,0 9.50598,-23.019 9.03761,0 L 216.70041,79.18747 203.88122,56.9664 227.42063,-2e-5 l -56.42865,0 -91.451568,0 -56.446015,0 z m 175.981796,68.58867 6.12336,10.59882 -36.9657,64.04383 30.84234,-74.64265 z m -147.654708,0.0694 30.44338,73.81003 -36.532055,-63.28059 6.088675,-10.52944 z"
            :id "cup"}]
    [:path {:style {:fill "#ffffff"}
            :d "M 34.526818,-51.55751 52.996969,21.61337 -3.3828659,-29.67722 47.907724,26.68531 -25.263158,8.21516 47.336482,28.79717 -25.263158,51.97574 49.223311,35.73862 -3.3828669,89.86811 50.746623,37.26193 34.526818,111.7484 57.688076,39.14876 78.270092,111.7484 59.79994,38.56021 116.17977,89.86811 64.889185,33.48828 138.04275,51.97574 65.443117,31.39373 138.04275,8.21516 63.573598,24.43496 116.17977,-29.67722 62.050286,22.91165 78.270092,-51.55751 55.091523,21.04213 34.526818,-51.55751 z"
            :id "shine"}]]])

(defn end-page
  []
  (let [{{:keys [player] :as winner} :winner
         {:keys [share-url]} :options
         :keys [players]} @app-state
        winner-name (players player)]
    [:div
     (if winner
       [:div#winner
        [winners-cup (get-in config [:colours player])]
        [:h3 "Vítězem se stává"]
        [:h1 {:class (name player)} winner-name]]
       [:h1 "Kdo nehraje, nevyhraje."])
     [:a.button {:href (or share-url "")} [:span "Hrát znovu"]]]))
