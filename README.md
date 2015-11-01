# db-quiz

DB-quiz is a game inspired by the television show [AZ-kvíz](https://cs.wikipedia.org/wiki/AZ-kv%C3%ADz) popular in the Czech Republic. The game can be played at <http://mynarz.net/db-quiz/>.

## How to play the game

<img src="https://raw.githubusercontent.com/jindrichmynarz/db-quiz/master/resources/public/img/example_board.png" alt="Example game board" align="right" />

DB-quiz is a simple knowledge-based game for 2 players who play via the same browser (*yes*, a multiplayer mode is something I think about). The game is played on a triangular board made of hexagonal fields. Each turn players select an available field that they want to acquire. Players acquire fields when they correctly answer the question associated with the field. Each question is posed as a description of the thing the players must guess. The goal of the game is get to own fields that continuously span all 3 sides of the board. The first player who achieves this goal wins the game. The starting player is chosen at random and then the players alternate. 

When a player picks a field a question is shown. The player sees an abbreviation of the answer (e.g., *"TBL"* for *"Tim Berners-Lee"*) and a description of the answer (e.g., *"TBL is an English computer scientist, best known as the inventor of the World Wide Web."*). When the question is displayed, the player has 45 seconds to make a guess. If the guess is correct, player wins the field and it is marked by the player's colour. If the guess is incorrect or the time to guess elapses without the player providing an answer, then the field is marked as missed and it is coloured with dark grey. Missed field can be acquired by the players without answering a question, so that when a player picks a previously missed field, it is immediately won. When a player submits a guess, it is compared with the correct answer and the verdict is shown informing whether the guess was correct or not. The guess does not need to match the answer exactly. For example, letters capitalization, insignificant typos, missing punctuation or diacritics usually does not make the guess judged as incorrect (e.g., *"tim berners lea"* still matches the answer *"Tim Berners-Lee"*). When half of the player's time for making a guess elapses without the player typing anything a hint is shown. A hint reveals a few letters from the correct answer. For example, when *"Tim Berners-Lee"* is the answer, the hint may look like *"T⏑⏑ Be⏑n⏑⏑⏑ L⏑⏑"*. 

## Game options

Before staring a game, players may configure several options. While basic options only allow to change players' names, the advanced options enable to set a data source used for generating the game's questions. The game offers 2 data sources.

The default one is the [Czech Wikipedia](https://cs.wikipedia.org) retrieved from its semantic version produced under the umbrella of the [DBpedia](http://dbpedia.org) project. If this data source is chosen, players may pick one or more domains from which the game's questions will be drawn. For example, players may restrict the game only to questions about musicians or artists in general. Furthemore, players can choose the difficulty of the questions ranging from easy to normal or hard. 

An alternative data source is an arbitrary Google Spreadsheet. Anyone can create a Google Spreadsheet with questions that will be used in the game. This allows to create domain-specific games or games intended for learning a particular set of associations. A spreadsheet that can be used as a data source for DB-quiz needs to contain columns named *"Label"* and *"Description"*. *"Label"* provides the correct answers, while *"Description"* provides the questions describing the things to be guessed. A valid spreadsheet must contain a header row with the column labels and at least 28 rows, so that there is a question for each field on the game board. When the spreadsheet contains more rows, then 28 rows are selected randomly. The spreadsheet needs to be published before it can be read by DB-quiz. To do so, go to *"File"* → *"Publish to the web..."* and click *"Publish"*. Now you can copy the spreadsheet's URL and paste it into the game's options menu. A fixed URL for the games based on the given spreadsheet will be created, which you can share with others. 

## Under the hood

DB-quiz is implemented as a client-side application written in [Clojurescript](https://github.com/clojure/clojurescript). It is built using the [Reagent](https://github.com/reagent-project/reagent) library, which provides a simple interface wrapping [React.js](http://facebook.github.io/react/).

Questions from DBpedia are retrieved via [SPARQL](http://www.w3.org/TR/sparql11-query/) queries. The queries request [JSON-P](http://json-p.org/) responses in order to avoid the single origin restriction for AJAX requests. To restrict the domains of questions, the game uses the RDF predicates `rdf:type` to partition the available resources by their class (e.g., `dbo:Film`) and `dcterms:subject` to partition category (e.g., `<http://cs.dbpedia.org/resource/Kategorie:Narození_v_Brně>` for persons born in Brno).

First, resources matching the selected predicates are ordered by their [indegree](https://en.wiktionary.org/wiki/indegree) that is computed from the number of links to the resource via the `dbo:wikiPageWikiLink` property. This property represents links to Wikipedia pages from the body text of other Wikipedia pages. We assume that indegree corresponds with how well the resources are known. For instance, the person that currently (August 29, 2015) has the largest indegree in Czech DBpedia is [Carl Linné](http://cs.dbpedia.org/resource/Carl_Linné).

Based on the chosen game difficulty, we pick a subset of the resource matching the selected predicates. First, we order the resources by their [indegree](https://en.wiktionary.org/wiki/indegree). Our assumption is that resources with high indegree are better known, so they are easier to guess. Since the indegree follows a long-tail distibution, there are much more difficult resources than the easy ones. Therefore, we split the distribution of resources at points where a third of indegree links is accumulated. 

The selected subset of resources is further filtered to match the game's criteria. Using regular expressions we exclude resources labelled with abbreviations (e.g., [*"R.E.M."*](http://cs.dbpedia.org/resource/R.E.M.)). We only retrieve resources with longer descriptions (more than 140 characters) and shorter labels (less than 40 characters), so that players have enough hints to make a correct guess and not have to type long labels.

In order to prevent spoilers from appearing in the game's questions, the query then retrieves surface forms of the selected resources. We use surface forms connected with the resources via several properties. Most surface forms are obtained via the `dbo:wikiPageWikiLinkText` property that contains the ways a resource is labelled in links pointing to it from other Wikipedia pages. In this way we obtain various spellings or declinations of the resource's label (e.g., for *"Tim Berners-Lee"* we get the Czech declination *"Tima Bernerse-Leea"*). Other properties used to identify surface forms of a given resource include `dcterms:title` or `dbo:birthName` (e.g., we can learn that the birth name of the Czech opera singer Emmy Destinn was Emilie Pavlína Věnceslava Kittlová). 

Once we obtain the necessary input for generating questions, descriptions and labels of the resources are normalized. Normalization includes trimming and collapsing whitespace characters or deleting parenthesized substrings. We abbreviate the resource's label to its initial letters (e.g., *"Tim Berners-Lee"* becomes *"TBL"*). Subsequently, we replace any occurrence of the label or other surface forms of the resource in its description by this abbreviation. Finally, we truncate the description on a complete sentence to fit 500 characters to prevent the players needing to read to much text and also giving away too many hints.

The guesses given by the players undergo a similar normalization procedure. When we compare the guess with the correct answer, both are normalized to increase the tolerance of the match. The normalization includes lower-casing, replacing characters with diacritics with their ASCII counterparts, and removing punctuation. Exact match between the normalized guess and the correct answer is not required. Instead we compare them by using the Jaro-Winkler string similarity metric. We use a high similarity threshold (0.94), so that only a few mismatching characters in the guess are tolerated. We chose this metric because it penalizes the mismatches near the beginning of the guess more than those near the end. The metric also takes into account the length of the compared strings, so that more errors are accepted for longer strings.

The game's board is a represented in a graph data structure. The algorithm for determining if winner of a game was found is based on depth-first search of this data structure. 

The layout of the game uses both [HTML5](http://www.w3.org/TR/html5/), [SVG](http://www.w3.org/TR/SVG/) and [Canvas API](http://www.w3.org/TR/2dcontext/). The game's board is rendered in SVG, while canvas is used for rendering most animations.

## Related work

* [Diploma thesis of Šárka Turečková](http://isis.vse.cz/zp/index.pl?podrobnosti_zp=51463)
* Bratsas, Charalampos \[et al.\]. *Semantic web game based learning: an i18n approach with Greek DBpedia*. Proceedings of the 2nd International Workshop on Learning and Education with the Web of Data. Lyon, 2012. Available from <http://ceur-ws.org/Vol-840/01-paper-23.pdf>.
* Waitelonis, Jörg \[et al.\]. WhoKnows?: evaluating linked data heuristics with a quiz that cleans up DBpedia. *International Journal of Interactive Technology and Smart Education*. Bingley: Emerald, 2011. Available from <http://www.hpi.uni-potsdam.de/fileadmin/hpi/FG_ITS/Semantic-Technologies/paper/Waitelonis2011b.pdf>

## Licence

Copyright © 2015 Jindřich Mynarz

Distributed under the Eclipse Public License.
