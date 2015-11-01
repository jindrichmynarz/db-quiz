(ns db-quiz.normalize-test
  (:require [cljs.test :refer-macros [are deftest is testing]]
            [db-quiz.normalize :as normalize]
            [clojure.string :as string]))

(deftest name-like?
  (testing "Detection of names"
    (are [name-like] (normalize/name-like? name-like)
         "Wayne Dogdson"
         "František Čihák"
         "Jiří Suchogrommit")
    (are [not-name-like] (not (normalize/name-like? not-name-like))
         "Bambi"
         "Sklerofant Olgoj Chorchoj"
         "Wallace")))

(deftest name->regex
  (testing "Middle names are captured"
    (are [name-like full-name description]
         (= (.indexOf (string/replace description (normalize/name->regex name-like) "") full-name) -1)
         "Jiří Suchogrommit"
         "Jiří Wallace Suchogrommit"
         "Když se Jiří Wallace Suchogrommit jednou ráno probudil z nepokojných snů..."
         
         "Jean Monnet"
         "Jean Omer Marie Gabriel Monnet"
         "Jean Omer Marie Gabriel Monnet was a French political economist and diplomat."
         
         "Thomas Tune"
         "Thomas James \"Tommy\" Tune"
         "Thomas James \"Tommy\" Tune is an American actor."
         
         "Peter Capaldi"
         "Peter Dougan Capaldi" 
         "Peter Dougan Capaldi is a Scottish actor and film director."
         
         "Jan Kotěra"
         "Jan Karel Zdenko Kotěra"
         "Jan Karel Zdenko Kotěra byl český architekt, urbanista, teoretik architektury, návrhář nábytku a malíř.")))
