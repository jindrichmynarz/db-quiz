(ns db-quiz.i18n
  (:require [db-quiz.state :refer [app-state]]
            [taoensso.tower :as tower :refer-macros [dict-compile*]]))

(def ^:private dictionary
  {:compiled-dictionary (dict-compile* "localization.clj")
   :fallback-locale :cs})

(def t
  (let [translate (tower/make-t dictionary)]
    (fn [k] (translate (:language @app-state) k)))) 
