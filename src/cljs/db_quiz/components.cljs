(ns db-quiz.components
  (:require [db-quiz.layout :refer [hex-triangle]]
            [db-quiz.logic :refer [init-board make-a-guess]]
            [db-quiz.model :as model] 
            [db-quiz.state :refer [app-state]]
            [db-quiz.util :refer [join-by-space redirect toggle]]
            [db-quiz.modals :as modals]
            [clojure.string :as string]
            [reagent.core :refer [atom]]
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
                        :type "text"}])

(defn timeout
  [on-turn completion]
  [:div.row
   [:div#timeout {:class (name on-turn)}]
   [:div#timeout-shade {:style {:margin-left (str completion "%")
                                :width (str (- 100 completion) "%")}}]])

(defn verdict-component
  []
  (let [{:keys [board current-field verdict]} @app-state
        correct-answer (get-in board [current-field :label]) 
        {:keys [glyphicon-class
                success
                verdict-class]} (if verdict
                                  {:glyphicon-class "glyphicon-ok"
                                   :success "Ano"
                                   :verdict-class "alert-success"}
                                  {:glyphicon-class "glyphicon-remove"
                                   :success "Ne"
                                   :verdict-class "alert-danger"})]
    [:div#verdict.row {:class (when (nil? verdict) "transparent")}
      [:div.col-sm-12
        [:p {:class (str "alert " verdict-class)}
          [:span {:class (join-by-space "glyphicon" "glyphicon-start" glyphicon-class)}]
          success ". Správná odpověď je " [:strong correct-answer] "."]]]))

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
      [verdict-component]
      ;[:div.row
      ;  [:p.col-sm-12 [:strong "Řešení: "] label]]
      ]))

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
            player-name)]]
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
