(ns db-quiz.layout-test
  (:require [cemerick.cljs.test :refer-macros [is are deftest testing use-fixtures done]]
            [db-quiz.layout :as layout]))

(deftest shade-colour
  (testing "Comparing with reference implementation"
    (are [input-colour shade-percentage output-colour]
         (= (layout/shade-colour input-colour shade-percentage) output-colour)
         "#fa0000" 0 "#fa0000" ; With 0 shade percentage, the colour must stay the same.
         "#fa0000" 30 "#ff4d4d"
         "#fa0000" -30 "#ae0000")))

(deftest hex-corner
  (let [center (repeatedly 2 #(rand-int 1000))
        size (rand-int 100)
        my-hex-corner (partial layout/hex-corner center size)]
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
