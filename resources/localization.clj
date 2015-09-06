{:cs {:end {:no-winner "Kdo nehraje, nevyhraje."
            :play-again "Hrát znovu"
            :winner "Vítězem se stává"}
      :home {:advanced-options "Pokročilé nastavení"
             :data-source "Zvolte zdroj otázek:"
             :dbpedia "DBpedie"
             :difficulty {:easy "Jednoduchá"
                          :hard "Obtížná"
                          :label "Obtížnost"
                          :normal "Běžná"}
             :domains {:artists "Umělci"
                       :born-in-brno "Narození v Brně"
                       :companies "Firmy"
                       :films "Filmy"
                       :ksc-members "Členové KSČ"
                       :label "Druhy otázek"
                       :languages "Jazyky"
                       :musicians "Hudebníci"
                       :places "Místa"
                       :persons "Osoby"
                       :politicians "Politici"
                       :software "Software"
                       :uncertain-death "Osoby s nejistým datem úmrtí"
                       :works "Díla"}
             :field-labelling "Označování políček"
             :game-url "URL hry"
             :google-spreadsheet "Google Spreadsheet"
             :language "Jazyk"
             :play "Hrát"
             :player-1! "1. hráč"
             :player-2! "2. hráč"
             :player-name "Jméno hráče"}
      :labels {:about "O hře"
               :home "Domů"
               :loading "Načítání"
               :logo "Logo DB-quizu"}
      :messages {:no-difficulty-selected "Musí být zvolena obtížnost."
                 :no-domain-selected "Alespoň 1 druh otázek musí být zvolen."
                 :no-field-labelling-selected "Musí být zvolen způsob označování políček."
                 :no-spreadsheet-url "URL Google Spreadsheetu nesmí být prázdné."
                 :player-1-missing "Chybí jméno 1. hráče."
                 :player-2-missing "Chybí jméno 2. hráče."}
      :modals {:error-sparql-load! "Chyba při načítání dat ze SPARQL endpointu <a href=\"{{sparql-endpoint}}\">{{sparql-endpoint}}</a>. Zkuste snížit počet okruhů otázek nebo hru znovu načíst."
               :game-info* "## Pravidla hry ##

Hra je založena na známé televizní soutěži [AZ-kvíz](https://cs.wikipedia.org/wiki/AZ-kv%C3%ADz). Jejím cílem je obsadit pole souvisle spojující všechny 3 strany hrací plochy. Hráč získá pole v případě, kdy správně zodpoví položenou otázku. Každá otázka obsahuje popis hádané věci a zkratku jejího názvu. Pokud je otázka pro zvolené pole zodpovězena nesprávně, pole je označeno jako neuhodnuté a jakýkoli hráč na tahu ho může získat bez nutnosti zodpovídání otázky. Na zodpovězení otázky má každý hráč 45 sekund. První hráč, který svými poli souvisle propojí všechny 3 strany hrací plochy, se stává vítězem. Začíná náhodně vylosovaný hráč.

## Jak hra funguje? ##

V případě DB-quizu jsou herní otázky náhodně generovány na základě databáze. Ve hře je jako databáze použita sémantická podoba české Wikipedia, která je vytvářena v projektu [DBpedia](http://dbpedia.org/). Alternativně lze použít otázky z tabulek v Google Spreadsheets.
                           
Hru dal dohromady [Jindřich Mynarz](http://mynarz.net/#jindrich) s pomocí přátel. Zdrojový kód hry je k dispozici [zde](https://github.com/jindrichmynarz/db-quiz)."
               :google-spreadsheet-help* "## Jak má tabulka vypadat? ##

Tabulka musí mít 2 sloupce, záhlaví a alespoň tolik řádků, kolik je hracích políček. Ve sloupci pojmenovaném 'Label' jsou názvy hádaných věcí, zatímco ve sloupci se jménem 'Description' jsou otázky popisující hádanou věc.
 
## Jak tabulku publikovat? ##
 
Zvolte *File* → *Publish to the web...* a klikněte na *Publish*."
               :invalid-number-of-results! "Načteno nesprávné množství dat. Hra potřebuje {{expected}} položek, ale jen {{actual}} bylo načteno. Zkuste rozšířit okruhy otázek."
               :invalid-options "Chyby v nastavení"
               :invalid-spreadsheet-columns "Sloupce v tabulce nejsou správně pojmenovány. Sloupec s hádanou věcí musí nést jméno 'Label', zatímco sloupec s otázkou je pojmenován 'Description'."
               :invalid-spreadsheet-rows! "Nesprávný počet řádků v tabulce. Je třeba alespoň {{number-of-fields}} řádků, ale tabulka má jen {{actual}} řádků."
               :invalid-spreadsheet-url! "Neplatné URL Google Spreadsheetu: <{{url}}>"
               :offline-warning "Jste offline. Hra funguje pouze s připojením k internetu."
               :report-error! "<a href=\"https://github.com/jindrichmynarz/db-quiz/issues\">
  <span class=\"glyphicon glyphicon-exclamation-sign glyphicon-start\"></span> Nahlásit problém hry
</a>"}
      :play {:answer "Odpověď"
             :correct-answer "Správná odpověď je"
             :guess "Odpovědět"
             :skip "Dál"
             :skip-title "Nevim, dál!"
             :verdict {:no "Ne"
                       :yes "Ano"}}}
 :en {:end {:no-winner "Who plays, wins."
            :play-again "Play again"
            :winner "The winner is"}
      :home {:advanced-options "Advanced options"
             :data-source "Choose a data source:"
             :dbpedia "DBpedia"
             :difficulty {:easy "Easy"
                          :hard "Hard"
                          :label "Difficulty"
                          :normal "Normal"}
             :domains {:artists "Artists"
                       :born-in-brno "Born in Brno"
                       :companies "Companies"
                       :films "Films"
                       :ksc-members "Members of the Communist Party"
                       :label "Domains of questions"
                       :languages "Languages"
                       :musicians "Musicians"
                       :places "Places"
                       :persons "People"
                       :politicians "Politicians"
                       :software "Software"
                       :uncertain-death "People with uncertain date of death"
                       :works "Works"}
             :field-labelling "Field labelling"
             :game-url "URL of the game"
             :google-spreadsheet "Google Spreadsheet"
             :language "Language"
             :play "Play"
             :player-1! "Player 1"
             :player-2! "Player 2"
             :player-name "Player's name"}
      :labels {:about "About the game"
               :home "Home"
               :loading "Loading"
               :logo "DB-quiz logo"}
      :messages {:no-difficulty-selected "A game difficulty must be chosen."
                 :no-domain-selected "At least 1 domain of questions must be chosen"
                 :no-field-labelling-selected "A way fields are labelled must be chosen."
                 :no-spreadsheet-url "Google Spreadsheet URL must not be empty."
                 :player-1-missing "Missing name of the player 1."
                 :player-2-missing "Missing name of the player 2."}
      :modals {:error-sparql-load! "Error loading data from a SPARQL endpoint <a href=\"{{sparql-endpoint}}\">{{sparql-endpoint}}</a>. Try lowering the number of domains of questions or reloading the game"
               :game-info* "## Rules of the game ##

DB-quiz is based on TV show [AZ-kvíz](https://cs.wikipedia.org/wiki/AZ-kv%C3%ADz), which is well-known in the Czech Republic. The game is played on a triangular board made of hexagonal fields. Each turn players select an available field that they want to acquire. Players acquire fields when they correctly answer the question associated with the field. Each question is posed as a description of the thing the players must guess. The goal of the game is get to own fields that continuously span all 3 sides of the board. The first player who achieves this goal wins the game. The starting player is chosen at random and then the players alternate.

## How does the game work? ##

DB-quiz generates game questions randomly from a database. The game uses the semantic form of Czech Wikipedie that is created as a part of [DBpedia](http://dbpedia.org/). Google Spreadsheet can be used as an alternative source of questions. 

The game was created by [Jindřich Mynarz](http://mynarz.net/#jindrich) with help from his friends. Source code of the game is available [here](https://github.com/jindrichmynarz/db-quiz)."
               :google-spreadsheet-help* "## How should I format the spreadsheet? ##

A spreadsheet that can be used as a data source for DB-quiz needs to contain columns named *Label* and *Description*. *Label* provides the correct answers, while *Description* provides the questions describing the things to be guessed. A valid spreadsheet must contain a header row with the column labels and at least 28 rows, so that there is a question for each field on the game board. When the spreadsheet contains more rows, then 28 rows are selected randomly.
 
## How to publish the spreadsheet? ##

The spreadsheet needs to be published before it can be read by DB-quiz. To do so, go to *File* → *Publish to the web...* and click *Publish*."
               :invalid-number-of-results! "Insufficient amount of data was loaded. The game needs {{expected}} questions, but only {{actual}} were loaded. Try expanding the domain of questions."
               :invalid-options "Invalid options"
               :invalid-spreadsheet-columns "Columns in the spreadsheet are not named correctly. The column with things to guess must be named 'Label', while the column with questions must be named 'Description'."
               :invalid-spreadsheet-rows! "The number of rows in the spreadsheet is invalid. The game requires at least {{number-of-fields}} rows, but the spreadsheet contains only {{actual}} rows"
               :invalid-spreadsheet-url! "Invalid URL of a Google Spreadsheet: <{{url}}>"
               :offline-warning "You are offline. The game works only with internet connection."
               :report-error! "<a href=\"https://github.com/jindrichmynarz/db-quiz/issues\">
  <span class=\"glyphicon glyphicon-exclamation-sign glyphicon-start\"></span> Report an error of the game
</a>"}
      :play {:answer "Answer"
             :correct-answer "The correct answer is"
             :guess "Make a guess"
             :skip "Skip"
             :skip-title "Man, I really don't know!"
             :verdict {:no "No"
                       :yes "Yes"}}}}
