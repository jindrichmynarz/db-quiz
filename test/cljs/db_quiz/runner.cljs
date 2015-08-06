(ns db-quiz.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [db-quiz.layout-test]))

(doo-tests 'db-quiz.layout-test)
