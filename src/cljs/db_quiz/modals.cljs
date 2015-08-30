(ns db-quiz.modals
  (:require [db-quiz.util :refer [number-of-fields]]
            [db-quiz.i18n :refer [t]]
            [reagent-modals.modals :refer [close-modal!]]))

(def pos-number?
  (every-pred number? pos?))

(defn modal
  "Wraps modal body in a div with closing button."
  [body]
  [:div
   [:button.close {:on-click close-modal!}
    [:span "×"]]
   body])

(defn wrap-html
  "Wraps raw HTML to prevent escaping via React."
  [html]
  [:div {:dangerouslySetInnerHTML {:__html html}}])

(defn error-loading-data
  [sparql-endpoint]
  (modal
    [:div (t :modals/error-sparql-load)
     [:a {:href sparql-endpoint} sparql-endpoint]
     ". " (t :modals/error-sparql-load-hint)]))

(defn game-info
  []
  (modal (wrap-html (str (t :modals/game-info)
                         (t :modals/report-error)))))

(defn google-spreadsheet-help
  []
  (modal (wrap-html (t :modals/google-spreadsheet-help))))

(defn invalid-google-spreadsheet-url
  [url]
  (modal [:div (t :modals/invalid-spreadsheet-url) " \"" url "\"."]))

(defn invalid-number-of-results
  [expected actual]
  (modal [:div (t :modals.invalid-number-of-results/p1)
               expected
               (t :modals.invalid-number-of-results/p2)
               actual
               (t :modals.invalid-number-of-results/p3)]))

(defn invalid-options
  [errors]
  (modal [:div
          [:h2 [:span.glyphicon.glyphicon-exclamation-sign.glyphicon-start]
           (t :modals/invalid-options)]
          [:ul (for [error errors]
                 [:li {:key error} error])]]))

(defn invalid-spreadsheet-columns
  []
  (modal [:div (t :modals/invalid-spreadsheet-columns)]))

(defn invalid-spreadsheet-rows
  [actual]
  {:pre [(pos-number? actual)]}
  (modal [:div (t :modals.invalid-spreadsheet-rows/p1)
               number-of-fields
               (t :modals.invalid-spreadsheet-rows/p2)
               actual
               (t :modals.invalid-spreadsheet-rows/p3)]))

(defn offline
  []
  (modal [:div (t :modals/offline-warning)]))
