(ns db-quiz.components
  (:require [db-quiz.layout :refer [hex-triangle]]
            [db-quiz.logic :refer [make-a-guess]]
            [db-quiz.model :as model] 
            [db-quiz.state :refer [app-state]]
            [reagent.core :as reagent]
            [reagent-forms.core :refer [bind-fields]]))

; ----- Common components -----

(defn label-element
  [for-id text]
  [:label.col-sm-2.control-label {:for for-id} text])

; ----- Home page -----

(defn player-form-field
  [id label]
  (let [local-id (keyword (str "players." id))]
    [:div.form-group [label-element local-id label] 
                     [:div.col-sm-10
                       [:input.form-control {:field :text :id local-id :max-length 20 :type "text"}]
                       [:div.alert.alert-danger
                         {:field :alert :id local-id :event empty?}
                         "Jméno hráče musí být vyplněno!"]]]))

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

(defn home-page
  []
  [:div.container-fluid
   [:div#logo [:img {:alt "DB quiz logo"
                     :src "/img/logo.svg"}]]
   [:div.col-sm-6.col-sm-offset-3
    [bind-fields start-menu app-state]]])

; ----- Play page -----

(defn loading-indicator
  []
  (when (:loading? @app-state)
    [:div#loading
     [:div#loadhex]
     [:p.vcenter "Načítání..."]]))

(defn friend-source [text]
  (filter
   #(-> % (.toLowerCase %) (.indexOf text) (> -1))
   ["Alice" "Alan" "Bob" "Beth" "Jim" "Jane" "Kim" "Rob" "Zoe"]))

(def autocomplete
  ;[:div {:field           :typeahead
  ;       :id              :answer
  ;       :data-source     friend-source
  ;       :input-class     "form-control"
  ;       :list-class      "typeahead-list"
  ;       :item-class      "typeahead-item"
  ;       :highlight-class "highlighted"}])
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
          [:span {:class (str "glyphicon " glyphicon-class)}]
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
      [:div.row
        [:p.col-sm-12 [:strong "Řešení: "] label]]]))

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
    [loading-indicator]
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
         :keys [players]} @app-state
        winner-name (players player)]
    [:div
     (when winner
       [:div#winner
        [:p "Vítězem se stává"]
        [:h1 {:class (name player)} winner-name]])
     [:a.button {:href "/"} [:span "Hrát znovu"]]]))
