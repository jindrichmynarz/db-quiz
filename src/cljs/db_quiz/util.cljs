(ns db-quiz.util
  (:require [db-quiz.config :refer [config]]
            [clojure.string :as string]
            [cljs.core.async :refer [chan put!]]
            [goog.events :as events]))

(defn join-by-space
  "Join arguments by space"
  [& args]
  (string/join " " args))

(defn listen
  "Listen for event-type on element.
  Taken from <http://swannodette.github.io/2013/11/07/clojurescript-101/>."
  [element event-type]
  (let [out (chan)]
    (events/listen element event-type
                   (fn [event] (put! out event)))
    out))

(def number-of-fields
  (apply + (range (inc (:board-size config)))))

(defn redirect
  "Redirect to URL"
  [url]
  (set! (.-location js/window) url))

(defn toggle
  "Toggle between 2 values given the current value"
  [[one two] value]
  (if (= one value)
    two
    one))
