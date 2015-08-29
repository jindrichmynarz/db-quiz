# db-quiz

DB-quiz is a game inspired by the television show [AZ-kvíz](https://cs.wikipedia.org/wiki/AZ-kv%C3%ADz) popular in the Czech Republic. The game can be played at <http://mynarz.net/db-quiz/>.

## How to play the game

<img src="https://raw.githubusercontent.com/jindrichmynarz/db-quiz/master/resources/public/img/example_board.png" alt="Example game board" align="right" />

DB-quiz is a simple knowledge-based game for 2 players who play via the same browser (*yes*, a multiplayer mode is something I think about). The game is player on a triangular board made of hexagonal fields. Each turn players select an available field that they want to acquire. Players acquire fields when they correctly answer the question associated with the field. Each question is posed as a description of the thing the players must guess. The goal of the game is get to own fields that continuously span all 3 sides of the triangular board. The first player who achieves this goal wins the game.

When a player picks a field a question is shown. The player see an abbreviation of the answer (e.g., *"TBL"* for *"Tim Berners-Lee"*) and a description of the answer (e.g., *"TBL is an English computer scientist, best known as the inventor of the World Wide Web."*). When the question is displayed the player has 45 seconds to make a guess. If the guess is correct, player wins the field and it is marked by the player's colour. If the guess is incorrect or the time to guess elapses without the player providing an answer, then the field is marked as missed and it is coloured with dark grey. Missed field can be acquired by the players without answering a question, so that when a player picks a previously missed field, it is immediately won. When a player submits a guess, it is compared with the correct answer and the verdict is shown informing whether the guess was correct or not. The guess does not need to match the answer exactly. For example, letters capitalization, insignificant typos, missing interpunction or diacritics usually does not make the guess judged as incorrect (e.g., *"tim berners lea"* still matches the answer *"Tim Berners-Lee"*). When half of the player's time for making a guess elapses without the player typing anything a hint is shown. A hint reveals a few letters from the correct answer. For example, when *"Tim Berners-Lee"* is the answer, the hint may look like *"T⏑⏑ Be⏑n⏑⏑⏑ L⏑⏑"*. 

## Game options

Before staring a game, players may configure several options. While basic options only allow to change players' names, the advanced options enable to set a data source used for generating the game's questions. The game offers 2 data sources.

The default one is the [Czech Wikipedia](https://cs.wikipedia.org) retrieved from its semantic version produced under the umbrella of the [DBpedia](http://dbpedia.org) project. If this data source is chosen, players may pick one or more domains from which the game's questions will be drawn. For example, players may restrict the game only to questions about musicians or artists in general. Furthemore, players can choose the difficulty of the questions ranging from easy to normal or hard. Finally, it is possible to configure if the board should use numeric or alphabetic labels for the fields. If numeric labelling is chosen, then numbers from 1 to the number of fields on the board will be used. In the case alphabetic labelling is selected, then the fields on the board will be labelled from *A* to *Ž* and the capitals of the game's answers will correspond to the fields' labels. 

An alternative data source is an arbitrary Google Spreadsheet. Anyone can create a Google Spreadsheet with questions that will be used in the game. This allows to create domain-specific games or games intended for learning a particular set of associations. A spreadsheet that can be used as a data source for DB-quiz needs to contains columns named *"Label"* and *"Description"*. *"Label"* provides the correct answers, while *"Description"* provides the questions describing the things to be guessed. A valid spreadsheet must contain the header row with the column labels and at least 28 rows, so that there is a question for each field on the game board. When the spreadsheet contains more rows, then 28 rows are selected randomly. The spreadsheet needs to be published before it can be read by DB-quiz. To do so, go to *"File"* → *'Publish to the web...'* and click *'Publish'*. Now you can copy the spreadsheet's URL and paste it into the game's options menu. A fixed URL for the games based on the given spreadsheet will be created, which you can share with others. 

## Licence

Copyright © 2015 Jindřich Mynarz

Distributed under the Eclipse Public License.
