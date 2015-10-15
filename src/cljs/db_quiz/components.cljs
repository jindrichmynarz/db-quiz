(ns db-quiz.components
  (:require [db-quiz.logic :refer [annull-game! init-board make-a-guess randomize-despoilerification]]
            [db-quiz.model :as model]
            [db-quiz.state :refer [app-state]]
            [db-quiz.util :refer [join-by-space redirect toggle]]
            [db-quiz.modals :as modals]
            [db-quiz.config :refer [config]]
            [db-quiz.normalize :refer [trim-player-name]]
            [db-quiz.layout.svg :as svg]
            [db-quiz.layout.canvas :as canvas]
            [db-quiz.i18n :refer [t]]
            [db-quiz.analytics :as analytics]
            [clojure.string :as string]
            [cljs-http.client :refer [generate-query-string]]
            [goog.net.Cookies]
            [reagent.core :as reagent :refer [atom]]
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

(defn- btn-class-fn
  "Generate classes for a button based on whether it is active or not."
  [active?]
  (join-by-space "btn" "btn-default" (if active? "active" "")))

(defn mount-tooltip
  "Mounts a Twitter bootstrap tooltip onto an element.
  It will be destroyed after 10 seconds and we will store that it was shown in cookies."
  [tooltip-name element]
  (let [cookie-id (str "tooltip-" tooltip-name)
        shown? (.get cookies cookie-id)]
    (when (undefined? shown?)
      (let [element (js/$ (reagent/dom-node element))]
        (.tooltip element "show")
        (js/setTimeout (fn []
                         (.tooltip element "destroy")
                         (.set cookies cookie-id "true"))
                       10000)))))

; ----- Common components -----

(defn menu
  "Fixed information & navigazion menu"
  [& {:keys [home?]
      :or {home? false}}]
  [:div#info-menu.btn-group {:role "group"}
   (when-not home?
     [:a.btn.btn-default {:href "#"} [:span.glyphicon.glyphicon-home.glyphicon-start] (t :labels/home)])
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
        button (fn [current-id [id label]]
                 [:button {:class (btn-class-fn (= id current-id))
                           :data-key id
                           :key id}
                  (if (keyword? label) (t label) label)])]
    (fn [id label-key options]
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
  [id label-key options & {:keys [max-selected]}]
  (let [id-str (string/join "." (map name id))
        click (fn [current-id update-fn]
                (swap! app-state (fn [state] (update-in state id #(update-fn % current-id)))))]
    (fn [id label-key options]
      (let [current-value (get-in @app-state id)
            can-select-more? (or (nil? max-selected)
                                 (< (count current-value) max-selected))]
        [:div.form-group
         [label-element id-str (t label-key)]
         [:div.col-sm-8.btn-group-vertical {:id id-str
                                            :role "group"}
          (doall (for [[current-id label] options
                       :let [active? (current-value current-id)
                             update-fn (if active? disj conj)]]
                   [:button {:class (btn-class-fn active?)
                             :disabled (not (or active? can-select-more?))
                             :key label
                             :on-click (partial click current-id update-fn)}
                    (t label)]))]]))))

; ----- Home page -----

(defn cookies-warning
  "Display warning about the use of cookies."
  []
  (let [cookies? (.get cookies "cookies")
        hidden? (atom false)
        hide (fn [_] (reset! hidden? true)
                     (.set cookies "cookies" "true"))]
    (fn []
      (when-not (or cookies? @hidden?)
        [:div#cookies-warning
         [:p (t :home.cookies/warning)
          [:button.btn.btn-default {:on-click hide}
           [:span.glyphicon.glyphicon-start.glyphicon-chevron-right]
           (t :home.cookies/proceed-button)]]]))))

(defn loading-indicator
  "Indicates if data is loaded."
  []
  (when (:loading? @app-state)
    [:div#loading
     [:div#loadhex]
     [:p.vcenter (t :labels/loading) "..."]]))

(defn player-form-field
  "Form field for player's name given player id and label-key"
  [id label-key]
  (let [local-id (string/join "." ["players" (name id)])
        classes (join-by-space "form-group" "player-field" (name id))
        change (fn [e] (swap! app-state #(assoc-in % [:players id] (.. e -target -value))))]
    (fn []
      (let [player-name (get-in @app-state [:players id])]
        [:div {:class classes}
         [label-element local-id (t label-key)]
         [:div.col-sm-8
          [:input.form-control {:id local-id
                                :max-length 20
                                :on-change change
                                :placeholder (t :home/player-name)
                                :type "text"
                                :value player-name}]]]))))

(defn- set-google-spreadsheet-url
  [url]
  (letfn [(update-url [state] (assoc-in state [:options :doc] url))
          (update-share-url [state] (assoc-in state [:options :share-url] (generate-share-url url)))]
    (comp update-share-url update-url)))

(defn snm-switch
  []
  (let [update-fn (set-google-spreadsheet-url
                    "https://docs.google.com/spreadsheets/d/1XpkdSYMEEEBPurgfOEOPDn54AqT6uAGsZdEn9Nq0Vsk/edit")]
    (fn []
      [:button.btn.btn-default.btn-xs
       {:on-click (fn [e] (.preventDefault e)
                    (swap! app-state update-fn))}
       "SNM-" (t :home/quiz)])))

(defn google-spreadsheet-url
  "Input for URL of a Google Spreadsheet to load data from"
  []
  (letfn [(click-info [_] (reagent-modals/modal! (modals/google-spreadsheet-help)))
          (change [e] (let [url (.. e -target -value)
                            update-fn (set-google-spreadsheet-url url)]
                        (swap! app-state update-fn)))]
    (fn []
      (let [{{:keys [doc]} :options
             :keys [language]} @app-state]
        [:div.form-group
         [:p [:label.control-label {:for "options.doc"} "Google Spreadsheet URL"]
          [:a {:on-click click-info}
           [:span.glyphicon.glyphicon-question-sign.glyphicon-end]]]
         [:p
          [:input.form-control {:id :options.doc
                                :on-change change
                                :type "url"
                                :value doc}]]
        (when (= language :cs)
          [:p [:strong (t :home/example-spreadsheets) ": "] [snm-switch]])]))))

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
  [single-select [:options :labels]
                 :home/field-labelling
                 [[:numeric "0-9"]
                  [:alphabetic "A-Z"]]])

(defn language-picker
  []
  (let [options [[:cs "Česky"]
                 [:en "English"]]
        click-fn (fn [language]
                     (swap! app-state (comp #(assoc-in % [:options :selectors] #{})
                                            #(assoc % :language language)))
                     (.set cookies "language" (name language)))]
    (fn []
      (let [language (:language @app-state)]
        [:div.form-group
         [:div.btn-group.col-sm-4.col-sm-offset-9 {:role "group"}
          (doall (for [[id label] options
                       :let [active? (= id language)]]
                   [:button {:class (btn-class-fn active?)
                             :key id
                             :on-click (partial click-fn id)}
                     label]))]]))))

(defn difficulty-picker
  []
  [single-select [:options :difficulty]
                 :home.difficulty/label
                 [[:easy :home.difficulty/easy]
                  [:normal :home.difficulty/normal]
                  [:hard :home.difficulty/hard]]])

(defn selector-picker
  []
  (let [selectors {:cs [[:persons :home.domains/persons]
                        [:places :home.domains/places]
                        [:films :home.domains/films]
                        [:works :home.domains/works]
                        [:born-in-brno :home.domains/born-in-brno]
                        [:ksc-members :home.domains/ksc-members]
                        [:uncertain-death :home.domains/uncertain-death]
                        [:artists :home.domains/artists]
                        [:politicians :home.domains/politicians]
                        [:musicians :home.domains/musicians]]
                   :en [[:persons :home.domains/persons]
                        [:places :home.domains/places]
                        [:films :home.domains/films]
                        [:companies :home.domains/companies]
                        [:software :home.domains/software]
                        [:plants :home.domains/plants]
                        [:languages :home.domains/languages]
                        [:musicians :home.domains/musicians]
                        [:insects :home.domains/insects]
                        [:soccer-players :home.domains/soccer-players]]}]
    (fn []
      (let [language (:language @app-state)]
        [multi-select [:options :selectors]
                      :home.domains/label
                      (language selectors)
                      :max-selected 3]))))

(defn google-spreadsheet-options
  []
  [:div [google-spreadsheet-url]
        [share-url]])

(defn dbpedia-options
  []
  [:div [selector-picker]
        [difficulty-picker]])

(def advanced-options
  (let [id [:options :data-source]
        toggle-data-source (partial toggle [:dbpedia :gdrive])
        click-handler {:on-click (fn [_] (swap! app-state #(update-in % id toggle-data-source)))}
        activate (fn [active?] (if active? "active" ""))
        tab-pane-class (partial join-by-space "tab-pane")]
    (fn []
      (let [current (get-in @app-state id)
            language (:language @app-state)
            dbpedia-class (activate (= current :dbpedia))
            gdrive-class (activate (= current :gdrive))]
        [:div
         [:p (t :home/data-source)]
         [:ul.nav.nav-tabs
          [:li {:class dbpedia-class}
           [:a click-handler (t :home/dbpedia)]]
          [:li {:class gdrive-class}
           [:a click-handler (t :home/google-spreadsheet)]]]
         [:div.tab-content
          [:div {:class (tab-pane-class dbpedia-class)}
            [dbpedia-options]]
          [:div {:class (tab-pane-class gdrive-class)}
            [google-spreadsheet-options]]]]))))

(defn basic-options
  []
  [:div
   [language-picker]
   [player-form-field :player-1 :home/player-1]
   [player-form-field :player-2 :home/player-2]])

(def start-menu
  (let [options-hidden (atom true)]
    (set-defaults!)
    (fn []
      (annull-game!)
      [:div.col-sm-6.col-sm-offset-3
       [:div#start-menu.form-horizontal.row
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
                                    (do (randomize-despoilerification)
                                        (model/load-board-data (init-board)
                                                               (partial redirect "#play")))
                                    (reagent-modals/modal! (modals/invalid-options errors)))))}
         [:span (t :home/play)]]]])))

(defn home-page
  []
  (analytics/send-page-view "/")
  (fn []
    [:div
     [:div.container-fluid
      [loading-indicator]
      [menu :home? true]
      [:div#logo [:img {:alt (t :labels/logo)
                        :src "img/logo.svg"}]]
      [start-menu]]
     [reagent-modals/modal-window]
     [cookies-warning]]))

; ----- Play page -----

(defn report-spoiler
  "Button for reporting spoilers in questions."
  []
  (let [reported? (atom false)]
    (reagent/create-class
      {:component-did-mount (partial mount-tooltip "report-spoiler")
       :display-name "report-spoiler"
       :reagent-render (fn []
                         [:div#report-spoiler {:data-placement "left"
                                               :class (join-by-space "col-sm-4"
                                                                     (when @reported? "inactive"))
                                               :title (t :tooltips/report-spoiler)}
                          [:button.btn.btn-default
                           {:on-click (fn [e] (when-not @reported?
                                                (analytics/report-spoiler)
                                                (reset! reported? true))
                                        (.stopPropagation e))}
                           [:span {:class (join-by-space "glyphicon" "glyphicon-start"
                                                         (if @reported?
                                                           "glyphicon-ok"
                                                           "glyphicon-exclamation-sign"))}]
                           (if @reported?
                             (t :play/spoiler-reported)
                             (t :play/report-spoiler))]])})))

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

(defn answer-button
  []
  (reagent/create-class
    {:component-did-mount (partial mount-tooltip "answer-button")
     :display-name "answer-button"
     :reagent-render (fn []
                       [:button.btn.btn-primary
                        {:data-placement "bottom"
                         :on-click make-a-guess
                         :title (t :tooltips/answer-button)}
                        [:span.glyphicon.glyphicon-ok.glyphicon-start]
                        (t :play/guess)])}))

(defn timeout
  "Progress bar showing the ellapsed time from player's turn."
  [on-turn completion & {:keys [verdict?]}]
  (reagent/create-class
    {:component-did-mount (partial mount-tooltip "timeout")
     :display-name "timeout"
     :reagent-render (fn [on-turn completion & {:keys [verdict?]}]
                       [:div.row {:data-placement "left"
                                  :title (t :tooltips/timeout)}
                        [:div#timeout {:class (name on-turn)}]
                        [:div#timeout-shade {:class (if (and (not verdict?) (pos? completion))
                                                        "active"
                                                        "")}]])}))

(defn verdict-component
  "Show verdict if answer was correct or not."
  []
  (reagent/create-class
    {:component-did-mount (partial mount-tooltip "verdict")
     :display-name "verdict"
     :reagent-render (fn []
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
                         [:div#verdict.row {:data-placement "bottom"
                                            :title (t :tooltips/verdict)}
                          [:div.col-sm-12
                           [:div {:class (join-by-space "alert" verdict-class)}
                            [:p [icon]
                              success ". " (t :play/correct-answer) " " [:strong correct-answer] "."]
                            [:div.progress-bar]]]]))}))

(defn question-box
  "Box for presenting the question with given id."
  [id]
  (let [{{:keys [data-source]} :options
         :keys [board verdict]} @app-state
        {:keys [abbreviation description label]} (board id)]
    [:div
      [:div.row
        [:div.col-sm-12
          [:div.row
           [:div.col-sm-8 [:h2 abbreviation]]
           (when (= data-source :dbpedia) [report-spoiler])]
          [:p#description description]]]
      [:div.row
        [:div.col-sm-12
          [:div.input-group
            [guess]
            [:span.input-group-btn
             [answer-button]
             [:button.btn.btn-danger
              {:on-click make-a-guess
               :title (t :play/skip-title)}
              [:span.glyphicon.glyphicon-forward.glyphicon-start]
              (t :play/skip)]]]]]
      (when-not (nil? verdict) [verdict-component])]))

(defn player-on-turn
  "Show the name of the player, whose turn it currently is."
  []
  (let [{{:keys [completion start]} :timer
         :keys [on-turn players verdict]} @app-state
        player-name (on-turn players)
        [trimmed-name font-class] (trim-player-name player-name)]
    [:div
      [:div.row
        [:div#on-turn {:class (join-by-space (name on-turn) font-class)}
          trimmed-name
          (when (= player-name "Rybička") [canvas/easter-egg on-turn])]]
      [timeout on-turn completion :verdict? (not (nil? verdict))]]))

(defn play-page
  []
  (analytics/send-page-view "/play")
  (fn []
    (let [{:keys [board current-field]} @app-state]
      (if (empty? board)
        (do (redirect "#") ; If no data is loaded, redirect to home page.
            [:div]) ; Return empty div as a valid Reagent component.
        [:div.container-fluid
         [menu]
         [:div.row
          [:div.col-sm-5.col-sm-offset-1 [svg/hex-triangle]]
          [:div#question-box.col-sm-5
           [player-on-turn]
           (when current-field
             [question-box current-field])]]
         [reagent-modals/modal-window]]))))

; ----- End page -----

(defn facebook-button
  "Button to send a Facebook status about your game victories."
  [url]
  (let [app-id (if (zero? (.indexOf url "http://localhost")) "873059332790811" "873050382791706")]
     [:a.btn-hex {:href (str "https://www.facebook.com/dialog/feed/?"
                             (generate-query-string {:app_id app-id
                                                     :display "page"
                                                     :name "DB-quiz"
                                                     :caption (t :end/status)
                                                     :link url
                                                     :redirect_uri url}))}
      [:span.glyphicon.fa.fa-facebook]]))

(defn google+button
  "Button to share the game on Google+."
  [url]
  [:a.btn-hex {:href (str "https://plus.google.com/share?" (generate-query-string {:url url}))}
   [:span.glyphicon.fa.fa-google-plus]])

(defn tweet-button
  "Button to tweet about game victories."
  [url]
  (let [text (str (t :end/status) " " url)]
    [:a.btn-hex {:href (str "https://twitter.com/intent/tweet?" (generate-query-string {:text text}))}
     [:span.glyphicon.fa.fa-twitter]]))

(defn social-media-buttons
  "Buttons for sharing the game on social media."
  []
  (let [pathname js/window.location.pathname
        url (str js/window.location.origin (when-not (= pathname "/") pathname))]
    [:div#social-media
     [:h3 (t :end/share-victory)]
     [:div [tweet-button url]
           [google+button url]
           [facebook-button url]]]))

(defn end-page
  []
  (analytics/send-page-view "/end")
  (analytics/report-success-rate)
  (fn []
    (let [{{:keys [player] :as winner} :winner
           {:keys [share-url]} :options
           :keys [players]} @app-state
          winner-name (players player)]
      [:div
       (if winner
         [:div#winner
          [svg/winners-cup (get-in config [:colours player])]
          [:h3 (t :end/winner)]
          [:h1 {:class (name player)} winner-name]
          [social-media-buttons]]
         [:h1 (t :end/no-winner)])
       [:a.button {:href (or share-url "")} [:span (t :end/play-again)]]])))

; ----- Not found -----

(defn not-found
  []
  [:div
   [menu]
   [svg/not-found]
   [:h1 (t :labels/not-found)]])
