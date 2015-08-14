(ns db-quiz.model
  (:refer-clojure :exclude [replace])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [db-quiz.config :refer [config]]
            [db-quiz.state :refer [app-state]]
            [db-quiz.modals :as modals]
            [db-quiz.util :refer [number-of-fields]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! >! chan pipe]]
            [clojure.string :refer [join lower-case replace split trim]]
            [cljsjs.mustache :as mustache]
            [reagent-modals.modals :as reagent-modals]))

; ----- Private vars -----

(def load-labels-template
  "PREFIX dbo:     <http://dbpedia.org/ontology/>
  PREFIX rdfs:     <http://www.w3.org/2000/01/rdf-schema#>

  SELECT ?label
  WHERE {
    {
      SELECT (STR(?_label) AS ?label)
      WHERE {
        GRAPH <http://cs.dbpedia.org> {
          VALUES ?class {
            {{#classes}}
            <{{{.}}}>
            {{/classes}}
          }
          [] a ?class ;
            rdfs:label ?_label ;
            dbo:abstract ?_description .
          FILTER (!REGEX(?_label, '^(\\\\p{Lu}\\\\.?)+$')
                  &&
                  !REGEX(?_label, '^.*\\\\d+.*$') 
                  &&
                  (STRLEN(?_description) > 140)
                  &&
                  langMatches(lang(?_label), 'cs')
                  &&
                  langMatches(lang(?_description), 'cs'))
        }
      }
      ORDER BY ?label
    }
  }
  LIMIT {{limit}}
  OFFSET {{offset}}")

(def load-labels-group-template
  "PREFIX dbo:     <http://dbpedia.org/ontology/>
  PREFIX rdfs:     <http://www.w3.org/2000/01/rdf-schema#>

  SELECT (GROUP_CONCAT(STR(?_label); separator = '|') AS ?labels)
  WHERE {
    GRAPH <http://cs.dbpedia.org> {
      VALUES ?class {
        {{#classes}}
        <{{{.}}}>
        {{/classes}}
      }
      [] a ?class ;
        rdfs:label ?_label ;
        dbo:abstract ?_description .
      FILTER (!REGEX(?_label, '^(\\\\p{Lu}\\\\.?)+$')
              &&
              !REGEX(?_label, '^.*\\\\d+.*$') 
              &&
              (STRLEN(?_description) > 140)
              &&
              langMatches(lang(?_label), 'cs')
              &&
              langMatches(lang(?_description), 'cs'))
    }
  }")

; ----- Public functions -----

(def sparql-results-channel
  "Channel that extracts values from application/sparql-results+json format."
  (letfn [(extract-values [result] (into {} (map (fn [[k v]] [k (:value v)]) result)))]
    (fn []
      (chan 1 (map (comp (partial map extract-values) :bindings :results :body))))))

(defn sparql-query-channel
  "Virtuoso-specific JSON-P request"
  [endpoint query]
  (http/jsonp endpoint
              {:query-params {; Since JSON-P does not allow setting Accept header,
                              ; we need to use Virtuoso-specific query parameter `format`.
                              :format "application/sparql-results+json"
                              :query query
                              :timeout 60000}}))

(defn render-template
  "Render Mustache template with data."
  [template & {:keys [data]}]
  (.render js/Mustache template (clj->js data)))

(defn delete-parenthesized-parts
  [text]
  (-> text
      (replace #"\s*\([^)]+\)" "")
      (replace #"\s*\[[^\]]+\]" "")))

(defn clear-description
  "TODO: Implement cleaning"
  [description]
  description)

(defn truncate-description
  "Truncate description to the configured maximum length.
  Cuts the description on a complete sentence."
  [description]
  (let [maximum-length (:max-question-length config)]
    (cond (> (count description) maximum-length)
          (reduce (fn [a b]
                    (if (> (count a) maximum-length)
                        a
                        (str a ". " b)))
                  (split description #"\.\s+"))
          :else description)))

(def clear-label
  (comp trim delete-parenthesized-parts))

(defn clear-tokens
  "Filter out roman numerals"
  [tokens]
  (filter (fn [token]
            (not (re-matches #"^M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})\.?$" token)))
          tokens))

(defn tokenize
  "Split s into tokens delimited by whitespace."
  [s]
  (split s #"\s+|-"))

(defn abbreviate-tokens
  "Convert tokens into an abbreviation."
  [tokens]
  (apply str (map (fn [s] (if (zero? (.indexOf (lower-case s) "ch")) "Ch" (first s)))
                  tokens)))

(def abbreviate
  (comp abbreviate-tokens clear-tokens tokenize clear-label))

(defn replace-surface-forms
  "Replace a set of surface-forms appearing in description with abbreviation."
  [description abbreviation surface-forms]
  (loop [[surface-form & the-rest] surface-forms
         result description]
    (let [clean-result (replace result surface-form abbreviation)]
      (if-not the-rest
        clean-result
        (recur the-rest clean-result)))))

(defn clean-surface-form?
  "Predicate that validates a surface-form."
  [surface-form]
  ;(not (re-matches #"\s+" surface-form))
  true)

(defn despoilerify
  "Replace spoilers suggesting label from description"
  [{:keys [label description surfaceForms] :as item}]
  (let [clean-label (clear-label label)
        tokens (clear-tokens (tokenize clean-label))
        abbreviation (abbreviate-tokens tokens)
        ; Sort surface forms from the longest to the shortest, so that we first replace
        ; the longer matches. 
        surface-forms (sort-by (comp - count)
                               (conj (split surfaceForms "|") clean-label label))]
    (assoc item
           :abbreviation abbreviation
           :description (-> description
                            delete-parenthesized-parts
                            clear-description
                            (replace-surface-forms abbreviation surface-forms)
                            truncate-description)
           :label (join " " tokens))))

(defn wrap-load
  [input-channel output-channel]
  (go
    (swap! app-state #(assoc % :loading? true))
    (>! output-channel (<! input-channel))
    (swap! app-state #(assoc % :loading? false)))
  output-channel)

(defn sparql-query
  "Send a SPARQL query from file on query-path to sparql-endpoint."
  [sparql-endpoint query-path & {:keys [data]}]
  (let [query-channel (http/get query-path {:channel (chan 1 (map :body))})
        sparql-results (sparql-results-channel)]
    (go
      (swap! app-state #(assoc % :loading? true))
      (let [query (render-template (<! query-channel) :data data)]
        ; I wonder why cljs.pipe/pipe doesn't block.
        (>! sparql-results (<! (sparql-query-channel sparql-endpoint query))))
      (swap! app-state #(assoc % :loading? false)))
    sparql-results))

(def load-labels
  (let [{{classes :default} :classes
         :keys [page-size endpoint]} (get-in config [:data :sparql])
        render-fn (fn [offset] (render-template load-labels-template
                                                :data {:classes classes
                                                       :limit page-size
                                                       :offset offset}))
        query-fn (fn [offset] (sparql-query-channel endpoint (render-fn offset)))
        sparql-results (sparql-results-channel)
        results (atom [])]
    (fn []
      (go-loop [offset 0]
               (swap! app-state #(assoc % :loading? true))
               (let [results (<! (query-fn offset))]
                 (if results
                   (do (js/console.log (clj->js results))
                       (>! sparql-results results)
                       (recur (+ offset page-size)))
                   (js/console.log "Wut, no results?"))) 
               (swap! app-state #(assoc % :loading? false)))
      (go (let [labels (<! sparql-results)]
            (swap! results #(conj % labels)))) 
      results)))

; (defn sparql-autocomplete
;   "Autocomplete from SPARQL endpoint.
;   request: jQuery UI autocomplete's request object
;   response: channel to put the response to"
;   [request response]
;   (let [fragment (lower-case (.-term request))
;         sparql-results (sparql-results-channel)
;         query (render-template autocomplete-template
;                                :data {:classes (get-in config [:data :sparql :classes :default])
;                                       :fragment fragment})
;         sparql-query (sparql-query-channel query)]
;     (go (let [results (map :label (<! (wrap-load sparql-query sparql-results)))]
;           (response (clj->js results))))))

(defn spreadsheet-url-to-id
  "Extract ID from Google Spreadsheet URL"
  [url]
  (let [a (.createElement js/document "a")
        _ (set! (.-href a) url)
        path-name (.-pathname a)
        start "/spreadsheets/d/"]
    (when (zero? (.indexOf path-name start))
      (-> path-name
          (replace start "")
          (split #"/" 2)
          first))))

(defn load-gdocs-items
  "Load items from Google Spreadsheet's worksheet."
  [spreadsheet-url]
  (let [worksheet-id "od6" ; FIXME: Is this value used every time?
        spreadsheet-id (spreadsheet-url-to-id spreadsheet-url)
        url (str "https://spreadsheets.google.com/feeds/list/"
                 spreadsheet-id "/" worksheet-id "/public/full")
        transform-row (fn [{{label :$t} :gsx$label
                           {description :$t} :gsx$description}]
                            {:label label
                             :abbreviation (abbreviate label)
                             :description description})
        raw-results-chan (wrap-load (http/jsonp url {:query-params {:alt "json-in-script"}})
                                (chan 1 (map (comp (partial map transform-row) :entry :feed :body))))
        results-chan (chan)]
    (if spreadsheet-id
      (do (go (let [results (<! raw-results-chan)
                    results-count (count results)]
                (cond (not= results-count number-of-fields)
                        (reagent-modals/modal! (modals/invalid-spreadsheet-rows results-count))
                      ; TODO: More validation rules
                      :else (>! results-chan results))))
          results-chan)
      (reagent-modals/modal! (modals/invalid-google-spreadsheet-url spreadsheet-url)))))

(defn- merge-board-with-data
  "Merge data with questions into the initialized board."
  [board data]
  (into {}
        (map (fn [[k v] result] [k (merge v result)])
             board
             data)))

(defmulti load-board-data (fn [] (get-in @app-state [:options :data-source])))

(defmethod load-board-data :dbpedia
  [board callback]
  (let [{{:keys [classes difficulty language]} :options} @app-state
        [endpoint query-file] (case language
                                    :czech ["http://cs.dbpedia.org/sparql" "sparql/cs_dbpedia.mustache"]
                                    :english ["http://dbpedia.org/sparql" "sparql/en_dbpedia.mustache"])
        offset (+ (case difficulty
                        :easy 0
                        :normal 3750
                        :hard 8750)
                  (rand-int 2500))
        query-channel (sparql-query endpoint
                                    query-file 
                                    :data {:classes classes
                                           :limit number-of-fields
                                           :offset offset})]
    (go (let [results (map despoilerify (<! query-channel))]
          (swap! app-state #(assoc % :board (merge-board-with-data board results)))
          (callback)))))

(defmethod load-board-data :gdrive
  [board callback]
  (let [spreadsheet-url (get-in @app-state [:options :doc])]
    (go (let [results (<! (load-gdocs-items spreadsheet-url))]
          (swap! app-state #(assoc % :board (merge-board-with-data board results)))
          (callback)))))
