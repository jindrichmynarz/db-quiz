(ns db-quiz.modals
  (:require [db-quiz.util :refer [number-of-fields]]
            [reagent-modals.modals :refer [close-modal!]]))

(def pos-number?
  (every-pred number? pos?))

(defn modal
  "Wraps modal body in a div with closing button."
  [body]
  [:div
   [:button.close {:on-click close-modal!}
    [:span "×"]]
   body])

(defn error-loading-data
  [sparql-endpoint]
  (modal
    [:div "Chyba při načítání dat ze SPARQL endpointu "
     [:a {:href sparql-endpoint} sparql-endpoint]
     ". Zkuste hru znovu načíst."]))

(def game-info
  (modal
    [:div
     [:h2 "Pravidla hry"]
     [:p "Hra je založena na známé televizní soutěži "
      [:a {:href "https://cs.wikipedia.org/wiki/AZ-kv%C3%ADz"} "AZ-kvíz"]
      ". Cílem hry je obsadit pole souvisle spojující všechny 3 strany hrací plochy. Hráč získá pole v případě, kdy správně zodpoví položenou otázku. Každá otázka obsahuje popis hádané věci a zkratku jejího názvu. Pokud je otázka pro zvolené pole zodpovězena nesprávně, pole je označeno jako neuhodnuté a jakýkoli hráč na tahu ho může získat bez nutnosti zodpovídání otázky. Na zodpovězení otázky má každý hráč 45 sekund. První hráč, který svými poli souvisle propojí všechny 3 strany hrací plochy, se stává vítězem. Začíná náhodně vylosovaný hráč."]
     [:h2 "Jak hra funguje?"]
     [:p "V případě DB-quizu jsou herní otázky náhodně generovány na základě databáze. Ve hře je jako databáze použita sémantická podoba české Wikipedia zvaná "
      [:a {:href "http://cs.dbpedia.org/"} "DBpedia"]
      "."]
     [:p "Hru dal dohromady "
      [:a {:href "http://mynarz.net/#jindrich"} "Jindřich Mynarz"]
      " s pomocí přátel. Zdrojový kód hry je k dispozici "
      [:a {:href "https://github.com/jindrichmynarz/db-quiz"} "zde"] "."]
     [:p [:a {:href "https://github.com/jindrichmynarz/db-quiz/issues"}
          [:span.glyphicon.glyphicon-exclamation-sign.glyphicon-start]
          "Nahlásit problém hry"]]]))

(def google-spreadsheet-help
  (modal
    [:div 
     [:h2 "Jak má tabulka vypadat?"]
     [:p "Tabulka musí mít 2 sloupce, záhlaví a alespoň tolik řádků, kolik je hracích políček. Název prvního sloupce je 'Label', druhý sloupec je pojmenován 'Description'. V prvním sloupci jsou názvy hádaných věcí, zatímco v druhém sloupci je otázka popisující hádanou věc."]
     [:h2 "Jak tabulku publikovat?"]
     [:p "Zvolte 'File' \u2192 'Publish to the web...' \u2192 'Publish'."]]))

(defn invalid-google-spreadsheet-url
  [url]
  (modal [:div "Neplatné URL Google Spreadsheetu: " url "."]))

(defn invalid-spreadsheet-rows
  [actual]
  {:pre [(pos-number? actual)]}
  (modal [:div "Nesprávný počet řádků v tabulce. Je třeba alespoň "
          number-of-fields "řádků, ale tabulka má jen " actual " řádků."]))
