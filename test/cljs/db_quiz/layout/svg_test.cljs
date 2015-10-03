(ns db-quiz.layout.svg-test
  (:require [cljs.test :refer-macros [are deftest is testing]]
            [db-quiz.layout.svg :as svg]))

(deftest hex-corner
  (let [center (repeatedly 2 #(rand-int 1000))
        size (rand-int 100)
        my-hex-corner (partial svg/hex-corner center size)]
    ; Coordinates of hexagon's corners rotated by 360 degrees must be the same
    (are [degree inverse-degree]
         (= (my-hex-corner degree) (my-hex-corner inverse-degree))
         0 6
         5 -1)
    ; x coordinates of the aligned corners are the same. 
    (are [degree inverse-degree]
         (= (first (my-hex-corner degree)) (first (my-hex-corner inverse-degree)))
         0 3
         1 2
         4 5)
    ; y coordinates of the aligned corners are the same.
    (are [degree inverse-degree]
         (= (second (my-hex-corner degree)) (second (my-hex-corner inverse-degree)))
         1 5
         2 4)))
