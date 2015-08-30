(ns db-quiz.util
  (:require [db-quiz.config :refer [config]]
            [clojure.string :as string]
            [cljs.core.async :refer [chan put!]]
            [goog.events :as events]
            [cljsjs.mustache :as mustache]))

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

(defn now
  "Returns current time in seconds"
  []
  (/ (.getTime (js/Date.)) 1000))

(def number-of-fields
  (apply + (range (inc (:board-size config)))))

(defn redirect
  "Redirect to URL"
  [url]
  (set! (.-location js/window) url))

(defn render-template
  "Render Mustache template with data."
  [template & {:keys [data]}]
  (.render js/Mustache template (clj->js data)))

(defn shade-colour
  "Shade hexadecimal RGB colour by percent.
  Stolen from <http://stackoverflow.com/a/13542669/385505>."
  [colour percent]
  (let [fit-bounds (fn [n] (cond (< n 1) 0
                                 (> n 255) 255
                                 :else n))
        numeric (js/parseInt (.slice colour 1) 16)
        amount (.round js/Math (* 2.55 percent))
        R (fit-bounds (+ (bit-shift-right numeric 16) amount))
        G (fit-bounds (+ (bit-and (bit-shift-right numeric 8) 0x00FF) amount))
        B (fit-bounds (+ (bit-and numeric 0x0000FF) amount))]
    (str "#" (.slice (.toString (+ 0x1000000 (* R 0x10000) (* G 0x100) B) 16) 1))))

(defn toggle
  "Toggle between 2 values given the current value"
  [[one two] value]
  (if (= one value)
    two
    one))
