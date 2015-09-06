(ns db-quiz.components
  (:require [db-quiz.logic :refer [annull-game! init-board make-a-guess]]
            [db-quiz.model :as model] 
            [db-quiz.state :refer [app-state]]
            [db-quiz.util :refer [join-by-space redirect toggle]]
            [db-quiz.modals :as modals]
            [db-quiz.config :refer [config]]
            [db-quiz.normalize :refer [trim-player-name]]
            [db-quiz.layout.svg :as svg] 
            [db-quiz.layout.canvas :as canvas]
            [db-quiz.i18n :refer [t]]
            [clojure.string :as string]
            [goog.net.Cookies]
            [reagent.core :refer [atom]]
            [reagent-modals.modals :as reagent-modals]))

(defonce cookies (goog.net.Cookies. js/document))

(defn check-if-online
  "If browser is offline, show a warning modal.
  Warning: This is unreliable, since browsers implement it inconsistently."
  []
  (when-not js/navigator.onLine (reagent-modals/modal! (modals/offline))))

(defn generate-share-url
  "Generate persistent game URL based on Google Spreadsheet at the given URL." 
  [url] 
  (let [location (.-location js/window)]
    (str (.-origin location)
         (.-pathname location)
         "#?doc="
         (js/encodeURIComponent url))))

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
  "Set preferred language based on cookies and browser's settings.
  Only supports Czech and English with Czech as default."
  []
  (let [language (if-let [language (.get cookies "language")]
                   (keyword language)
                   (case (.slice (.-language js/navigator) 0 2)
                     "en" :en
                     "cs" :cs
                     :cs))]
    (swap! app-state #(assoc % :language language))))

(defn set-defaults!
  "Set default options on load."
  []
  (check-if-online)
  (set-hash-options!)
  (set-language!))

(defn validate-options
  []
  (let [{{:keys [data-source difficulty doc
                 labels selectors]} :options
         {:keys [player-1 player-2]} :players} @app-state
        name-errors [(when (empty? player-1) (t :messages/player-1-missing))
                     (when (empty? player-2) (t :messages/player-2-missing))]
        data-errors (case data-source
                      :dbpedia [(when-not (pos? (count selectors)) (t :messages/no-domain-selected))
                                (when-not difficulty (t :messages/no-difficulty-selected))
                                (when-not labels (t :messages/no-field-labelling-selected))]
                      :gdrive [(when (empty? doc) (t :messages/no-spreadsheet-url))])
        errors (concat name-errors data-errors)]
    {:valid? (every? nil? errors)
     :errors (remove nil? errors)}))

; ----- Common components -----

(defn menu
  "Fixed information & navigazion menu"
  []
  [:div#info-menu.btn-group {:role "group"}
   [:a.btn.btn-default {:href "#"} [:span.glyphicon.glyphicon-home.glyphicon-start] (t :labels/home)]
   [:button.btn.btn-default {:on-click #(reagent-modals/modal! (modals/game-info) {:size :lg})}
    [:span.glyphicon.glyphicon-info-sign.glyphicon-start] (t :labels/about)]])

(defn label-element
  "Label with text for input identified with for-id"
  [for-id text]
  [:label.col-sm-4.control-label {:for for-id} text])

(defn single-select
  "Single-select button group. Associates selected value into given id path in app-state.
  The group is labelled with label-key's value from the translation map.
  Values of the available buttons are given in options as a collection of [value label] pairs.
  Label can be either a string or a keyword that will be looked up in the translation map."
  [id label-key options]
  (let [id-str (string/join "." (map name id))
        click (fn [e] (swap! app-state #(assoc-in % id (keyword (.. e -target -dataset -key)))))
        btn-class (partial join-by-space "btn" "btn-default")
        button (fn [current-id [id label]]
                 [:button {:class (btn-class (when (= id current-id) "active"))
                           :data-key id
                           :key id} 
                  (if (keyword? label) (t label) label)])]
    (fn []
      (let [current (get-in @app-state id)]
        [:div.form-group
         [label-element id-str (t label-key)]
         [:div.btn-group.col-sm-8 {:id id-str
                                   :on-click click
                                   :role "group"}
          (doall (for [option options]
                   (button current option)))]]))))

(defn multi-select
  "Multi-select button group.Associates selected values into given id path in app-state.
  The group is labelled with label-key's value from the translation map.
  Values of the available buttons are given in options as a collection of [value label] pairs.
  Label can be either a string or a keyword that will be looked up in the translation map."
  [id label-key options]
  (let [id-str (string/join "." (map name id))
        click (fn [current-id update-fn]
                (swap! app-state (fn [state] (update-in state id #(update-fn % current-id)))))
        btn-class (fn [active?] (join-by-space "btn" "btn-default" (when active? "active")))]
    (fn []
      (let [current-value (get-in @app-state id)]
        [:div.form-group
         [label-element id-str (t label-key)]
         [:div.col-sm-8.btn-group-vertical {:id id-str
                                            :role "group"}
          (doall (for [[current-id label] options
                       :let [active? (current-value current-id)
                             update-fn (if active? disj conj)]]
                   [:button {:class (btn-class active?)
                             :key label
                             :on-click (partial click current-id update-fn)}
                    (t label)]))]]))))

; ----- Home page -----

(defn loading-indicator
  "Indicates if data is loaded."
  []
  (when (:loading? @app-state)
    [:div#loading
     [:div#loadhex]
     [:p.vcenter (t :labels/loading) "..."]]))

(defn player-form-field
  "Form field for player's name"
  [id label]
  (let [player-name (get-in @app-state [:players id])
        local-id (keyword (str "players." (name id)))]
    [:div {:class (join-by-space "form-group" "player-field" (name id))}
     [label-element local-id label] 
     [:div.col-sm-8
      [:input.form-control {:id local-id
                            :max-length 20
                            :on-change (fn [e]
                                         (swap! app-state #(assoc-in % [:players id] (.. e -target -value))))
                            :placeholder (t :home/player-name)
                            :type "text"
                            :value player-name}]]]))

(defn google-spreadsheet-url
  "Input for URL of a Google Spreadsheet to load data from"
  []
  [:div.form-group
   [:p [:label.control-label {:for "options.doc"} "Google Spreadsheet URL"]
         [:a {:on-click #(reagent-modals/modal! (modals/google-spreadsheet-help))}
          [:span.glyphicon.glyphicon-question-sign.glyphicon-end]]]
   [:p
    [:input.form-control {:id :options.doc
                          :on-change (fn [e]
                                       (let [value (.. e -target -value)
                                             update-fn (comp #(assoc-in % [:options :share-url]
                                                                          (generate-share-url value))
                                                             #(assoc-in % [:options :doc] value))]
                                         (swap! app-state update-fn)))
                          :type "url"}]]])

(defn share-url
  "URL of the game based on given Google Spreadsheet"
  []
  [:div.form-group
   [:p [:label.control-label {:for "options.share-url"} (t :home/game-url)]]
   [:p [:textarea.form-control {:id :options.share-url
                                :readOnly "readOnly"
                                :rows 4
                                :value (get-in @app-state [:options :share-url])}]]])

(defn field-labelling
  []
  (single-select [:options :labels]
                 :home/field-labelling
                 [[:numeric "0-9"]
                  [:alphabetic "A-Z"]]))

(defn language-picker
  []
  (let [language (:language @app-state)
        [cs-class en-class] (case language
                                  :cs ["active" ""]
                                  :en ["" "active"])
        click-fn (fn [e]
                   (let [language (.. e -target -dataset -key)]
                     (swap! app-state #(assoc % :language (keyword language)))
                     (.set cookies "language" language)))]
    [:div.form-group {:on-click click-fn}
     [:div.btn-group.col-sm-6.col-sm-offset-8 {:role "group"}
      [:button {:class (join-by-space "btn" "btn-default" cs-class)
                :data-key :cs}
       "Česky"]
      [:button {:class (join-by-space "btn" "btn-default" en-class)
                :data-key :en}
       "English"]]]))

(defn difficulty-picker
  []
  (single-select [:options :difficulty]
                 :home.difficulty/label
                 [[:easy :home.difficulty/easy]
                  [:normal :home.difficulty/normal]
                  [:hard :home.difficulty/hard]]))

(defn selector-picker
  []
  (multi-select [:options :selectors]
                :home.domains/label
                [[:persons :home.domains/persons]
                 [:places :home.domains/places]
                 [:works :home.domains/works]
                 [:born-in-brno :home.domains/born-in-brno]
                 [:ksc-members :home.domains/ksc-members]
                 [:uncertain-death :home.domains/uncertain-death]
                 [:artists :home.domains/artists]
                 [:politicians :home.domains/politicians]
                 [:musicians :home.domains/musicians]
                 [:films :home.domains/films]]))

(defn google-spreadsheet-options
  []
  [:div [google-spreadsheet-url]
        [share-url]])

(defn dbpedia-options
  []
  [:div [selector-picker]
        [difficulty-picker]
        [field-labelling]])

(def advanced-options
  (let [toggle-data-source (partial toggle [:dbpedia :gdrive])
        click-handler {:on-click (fn [_] (swap! app-state
                                                #(update-in %
                                                            [:options :data-source]
                                                            toggle-data-source)))}
        tab-pane-class (partial join-by-space "tab-pane")]
    (fn []
      (let [[dbpedia-class gdrive-class] (case (get-in @app-state [:options :data-source])
                                               :dbpedia ["active" ""]
                                               :gdrive ["" "active"])]
        [:div
         [:p (t :home/data-source)]
         [:ul.nav.nav-tabs 
          [:li {:class dbpedia-class} [:a click-handler "DBpedia"]]
          [:li {:class gdrive-class} [:a click-handler "Google Spreadsheet"]]]
         [:div.tab-content
          [:div {:class (tab-pane-class dbpedia-class)}
            [dbpedia-options]]
          [:div {:class (tab-pane-class gdrive-class)}
            [google-spreadsheet-options]]]]))))

(defn basic-options
  []
  [:div
   [language-picker]
   [player-form-field :player-1 (t :home/player-1)]
   [player-form-field :player-2 (t :home/player-2)]])

(def start-menu
  (let [options-hidden (atom true)]
    (set-defaults!)
    (fn []
      (annull-game!)
      [:div.col-sm-6.col-sm-offset-3
       [:div#start-menu.form-horizontal
        [basic-options]
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
           (t :home/advanced-options)]]
         (when-not @options-hidden
           [advanced-options])]
        [:a.button {:on-click (fn [e]
                                (let [{:keys [valid? errors]} (validate-options)]
                                  (if valid?
                                    (model/load-board-data (init-board)
                                                           (partial redirect "#play"))
                                    (reagent-modals/modal! (modals/invalid-options errors)))))}
         [:span (t :home/play)]]]])))

(defn home-page
  []
  [:div.container-fluid
   [loading-indicator]
   [menu]
   [:div#logo [:img {:alt (t :labels/logo)
                     :src "img/logo.svg"}]]
   [start-menu]
   [reagent-modals/modal-window]])

; ----- Play page -----

(defn guess
  "Input field for guesses"
  []
  (let [{:keys [hint]} @app-state]
    [:input.form-control {:autoFocus "autoFocus"
                          :id :answer
                          :on-change (fn [e] (swap! app-state #(assoc % :answer (.-value (.-target e)))))
                          :on-key-down (fn [e]
                                         ; Submit a guess by pressing Enter
                                         (when (= (.-keyCode e) 13)
                                           (make-a-guess)))
                          :placeholder (or hint (t :play/answer)) 
                          :spellCheck "false"
                          :type "text"}]))

(defn timeout
  "Progress bar showing the ellapsed time from player's turn."
  [on-turn completion]
  [:div.row
   [:div#timeout {:class (name on-turn)}]
   [:div#timeout-shade {:style {:margin-left (str completion "%")
                                :width (str (- 100 completion) "%")}}]])

(defn verdict-component
  "Show verdict if answer was correct or not."
  []
  (let [{:keys [board current-field verdict]} @app-state
        correct-answer (get-in board [current-field :label]) 
        {:keys [icon
                success
                verdict-class]} (if verdict
                                  {:icon canvas/tick
                                   :success (t :play.verdict/yes)
                                   :verdict-class "alert-success"}
                                  {:icon canvas/cross
                                   :success (t :play.verdict/no) 
                                   :verdict-class "alert-danger"})]
    (when-not (nil? verdict)
      [:div#verdict.row
       [:div.col-sm-12
        [:p {:class (join-by-space "alert" verdict-class)}
         [icon]
         success ". " (t :play/correct-answer) " " [:strong correct-answer] "."]]])))

(defn question-box
  "Box for presenting the question with given id."
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
            [:span.input-group-btn
             [:button.btn.btn-primary
              {:on-click make-a-guess}
              [:span.glyphicon.glyphicon-ok.glyphicon-start]
              (t :play/guess)]
             [:button.btn.btn-danger
              {:on-click make-a-guess
               :title (t :play/skip-title)}
              [:span.glyphicon.glyphicon-forward.glyphicon-start]
              (t :play/skip)]]]]]
      [verdict-component]]))

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
          (when (= player-name "Rybička") [canvas/easter-egg on-turn])]]
      [timeout on-turn completion]]))

(defn play-page
  []
  (let [{:keys [board current-field]} @app-state]
    (if (empty? board)
      (do (redirect "#") ; If no data is loaded, redirect to home page.
          [:div]) ; Return empty div as a valid Reagent component. 
      [:div.container-fluid
       [menu]
       [:div.row
        [:div.col-sm-6 [svg/hex-triangle]]
        [:div#question-box.col-sm-6
         [player-on-turn]
         (when current-field
           [question-box current-field])]]
       [reagent-modals/modal-window]]))) 

; ----- End page -----

(defn end-page
  []
  (let [{{:keys [player] :as winner} :winner
         {:keys [share-url]} :options
         :keys [players]} @app-state
        winner-name (players player)]
    [:div
     (if winner
       [:div#winner
        [svg/winners-cup (get-in config [:colours player])]
        [:h3 (t :end/winner)]
        [:h1 {:class (name player)} winner-name]]
       [:h1 (t :end/no-winner)])
     [:a.button {:href (or share-url "")} [:span (t :end/play-again)]]]))
