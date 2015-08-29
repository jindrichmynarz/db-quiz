# db-quiz

<img src="https://raw.githubusercontent.com/jindrichmynarz/db-quiz/master/resources/public/img/logo.png" alt="DB-quiz logo" align="right" />

DB-quiz is a game inspired by the television show [AZ-kvíz](https://cs.wikipedia.org/wiki/AZ-kv%C3%ADz) popular in the Czech Republic. The game can be played at <http://mynarz.net/db-quiz/>.

## How to play the game

DB-quiz is a knowledge-based game for 2 players who play via the same browser (yes, multiplayer mode is something I think about). The game is player on a triangular board made of hexagonal fields. Each turn players select an available field that they want to acquire. Players acquire fields when they correctly answer the question associated with the field. Each question is posed as a description of the thing the players must guess. The goal of the game is get to own fields that continuously span all 3 sides of the triangular board. The first player who achieves this goal wins the game.

When a player picks a field a question is shown. The player see an abbreviation of the answer (e.g., *"TBL"* for *"Tim Berners-Lee"*) and a description of the answer (e.g., *"TBL is an English computer scientist, best known as the inventor of the World Wide Web."*). When the question is displayed the player has 45 seconds to make a guess. If the guess is correct, player wins the field and it is marked by the player's colour. If the guess is incorrect or the time to guess elapses without the player providing an answer, then the field is marked as missed and it is coloured with dark grey. Missed field can be acquired by the players without answering a question, so that when a player picks a previously missed field, it is immediately won. When a player submits a guess, it is compared with the correct answer and the verdict is shown informing whether the guess was correct or not. The guess does not need to match the answer exactly. For example, letters capitalization, insignificant typos, missing interpunction or diacritics usually does not make the guess judged as incorrect (e.g., *"tim berners lea"* still matches the answer *"Tim Berners-Lee"*). When half of the player's time for making a guess elapses without the player typing anything a hint is shown. A hint reveals a few letters from the correct answer. For example, when *"Tim Berners-Lee"* is the answer, the hint may look like *"T⏑⏑ Be⏑n⏑⏑⏑ L⏑⏑"*. 

## Licence

Copyright © 2015 Jindřich Mynarz

Distributed under the Eclipse Public License.
