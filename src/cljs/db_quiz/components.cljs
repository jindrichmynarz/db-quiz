(ns db-quiz.components
  (:require [db-quiz.layout :refer [hex-triangle]]
            [db-quiz.logic :as logic]
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

(defn autocomplete-did-mount []
  (js/$ (fn []
          (.autocomplete (js/$ "#guess") 
                         (clj->js {:delay 1000
                                   :minLength 3
                                   :source model/wikipedia-autocomplete})))))

(defn loading-indicator
  []
  (when (:loading? @app-state)
    [:div#loading
     [:div#loadhex]
     [:p.vcenter "Načítání..."]]))

(def autocomplete
  [:input#guess.form-control.ui-widget {:autoFocus "autoFocus" :field :text :id :answer :type "text"}])

(defn autocomplete-component []
  (reagent/create-class {:reagent-render autocomplete
                         :component-did-mount autocomplete-did-mount}))

(defn question-box
  [board id]
  (let [{:keys [abbreviation description label]} (@board id)]
    [:div#question-box.col-sm-6
      [:div.row
        [:div.col-sm-12
          [:h2 abbreviation]
          [:p#description description]]]
      [:div.row
        [label-element "guess" "Odpověď"]]
      [:div.row
        [:div.col-sm-12
        [:div.input-group
          [bind-fields autocomplete app-state]
          [:div.input-group-btn {:role "group"}
            [:button.btn.btn-primary
              {:on-click (partial logic/answer-question board id label)
               :title "Odpovědět"}
              [:span.glyphicon.glyphicon-ok]]
            [:button.btn.btn-danger
              {:on-click (partial logic/skip-question board id)
               :title "Nevim, dál!"}
              [:span.glyphicon.glyphicon-forward]]]]]]
      [:div.row
        [:p.col-sm-12 [:strong "Řešení: "] label]]]))

(defn player-on-turn
  "Show the name of the player, whose turn it currently is."
  []
  (let [{:keys [on-turn players]} @app-state
        player-name (on-turn players)
        player-name-length (count player-name)
        font-class (cond (< player-name-length 6) "font-large"
                         (< player-name-length 10) "font-regular"
                         :else "font-small")]
    [:div.col-sm-2.col-sm-offset-2
     [:p#on-turn-label "Na tahu je:"]
     [:p#on-turn {:class (str (name on-turn) " " font-class)}
       (if (> player-name-length 20)
         (str (subs player-name 0 17) "...")
         player-name)]]))

(defn play-page
  [board]
  [:div.container-fluid
   [loading-indicator]
   [:div.row
    [:div.col-sm-6 [hex-triangle board]]
    (when-let [current-field (:current-field @app-state)]
      [question-box board current-field])]
   [:div.row
    [player-on-turn]]])

; ----- End page -----

(defn end-page []
  [:div
    [:h2 "Všechno jednou končí..."]
    [:p "Hamižnost opět zvítězila nad pravdomluvností."]
    [:a.button {:href "/"} [:span "Hrát znovu"]]])
