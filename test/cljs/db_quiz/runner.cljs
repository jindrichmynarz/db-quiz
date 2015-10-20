(ns db-quiz.runner
  (:require [cljs.test :refer-macros [run-all-tests]]
            [doo.runner :refer-macros [doo-tests]]
            [db-quiz.layout.svg-test]
            [db-quiz.logic-test]
            [db-quiz.util-test]
            [db-quiz.normalize-test]))

(enable-console-print!)

(defn ^:export run
  []
  (run-all-tests #"db-quiz.*-test"))

(doo-tests 'db-quiz.layout.svg-test
           'db-quiz.logic-test
           'db-quiz.util-test
           'db-quiz.normalize-test)
