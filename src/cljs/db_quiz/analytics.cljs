(ns db-quiz.analytics
  "Functions wrapping the Google Analytics"
  (:require [db-quiz.state :refer [app-state]]))

(defn ga 
  "Wrap Google Analytics.
  Source: <https://coderwall.com/p/s3j4va/google-analytics-tracking-in-clojurescript>"
  [& more]
  (when js/ga
    (.. (aget js/window "ga")
        (apply nil (clj->js more)))))

(defn log-answer
  "Log if question about `subject-uri` was answered correctly."
  [subject-uri correct-answer?]
  (ga "send" "event" "answer" subject-uri (if correct-answer? "true" "false")))

(defn report-spoiler
  "Send a custom event to Google Analytics"
  []
  (let [{{:keys [despoilerify?]} :options
         :keys [board current-field language]} @app-state
        subject-uri (get-in board [current-field :subject])]
    (ga "send" "event" "report-spoiler" subject-uri (str (name language) "-"
                                                         (if despoilerify? "true" "false")))))

(defn report-success-rate
  "Report the rate of successfully answered questions at the given difficulty."
  []
  (let [{{:keys [correct incorrect]} :answers
         {:keys [data-source difficulty]} :options} @app-state]
    (when (and (= data-source :dbpedia)
               (some (complement zero?) [correct incorrect]))
      (ga "send"
          "event"
          "success-rate"
          (name difficulty)
          (str (/ correct (+ correct incorrect)))))))

(defn send-page-view
  "Sets `page` as the current page and sends a hit."
  [page]
  (ga "set" "page" page)
  (ga "send" "pageview"))
