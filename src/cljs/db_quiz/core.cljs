(ns db-quiz.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [db-quiz.components :as components]
            [reagent.core :as reagent]
            [reagent.session :as session]
            [secretary.core :as secretary :refer-macros [defroute]]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:import goog.History))

(enable-console-print!)
(.initializeTouchEvents js/React true)

;; -------------------------
;; Views

(secretary/set-config! :prefix "#")

(defn play-page []
  [components/play-page])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(defroute "/" []
  (session/put! :current-page components/home-page))

(defroute "/end" []
  (session/put! :current-page components/end-page))

(defroute "/play" []
  (session/put! :current-page play-page))

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
