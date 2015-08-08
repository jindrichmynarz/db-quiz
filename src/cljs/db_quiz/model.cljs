(ns db-quiz.model
  (:refer-clojure :exclude [replace])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [db-quiz.config :refer [config]]
            [db-quiz.state :refer [app-state]]
            [cljs-http.client :as http]
            [reagent.core :as reagent :refer [atom]]
            [cljs.core.async :refer [<! >! chan sliding-buffer]]
            [clojure.string :refer [join lower-case replace split trim]]
            [cljsjs.mustache :as mustache]))

; ----- Private vars -----

(def autocomplete-template
  "PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#>

  SELECT DISTINCT ?s (STR(?_label) AS ?label)
  WHERE {
    GRAPH <http://cs.dbpedia.org> {
      VALUES ?fragment {
        '{{fragment}}'
      }
      VALUES ?class {
        {{#classes}}
        <{{{.}}}>
        {{/classes}}
      }
      ?s a ?class ;
        rdfs:label ?_label .
      FILTER (lang(?_label) = '' || langMatches(lang(?_label), 'cs'))
      BIND (LCASE(?_label) AS ?lcaseLabel)
      FILTER CONTAINS(?lcaseLabel, ?fragment)
      BIND (IF(STRSTARTS(?lcaseLabel, ?fragment), 1, 0) AS ?order)
    }
  }
  ORDER BY DESC(?order)
  LIMIT 10")

; ----- Public functions -----

(defn sparql-results-channel
  "Channel that extracts values from application/sparql-results+json format."
  []
  (letfn [(extract-values [result] (into {} (map (fn [[k v]] [k (:value v)]) result)))]
    (chan 1 (map (comp (partial map extract-values) :bindings :results :body)))))

(defn sparql-query-channel
  "Virtuoso-specific JSON-P request"
  [query]
  (http/jsonp (get-in config [:data :sparql :endpoint])
              {:query-params {; Since JSON-P does not allow setting Accept header,
                              ; we need to use Virtuoso-specific query parameter `format`.
                              :format "application/sparql-results+json"
                              :query query}}))

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
  [description]
  description)

(defn truncate-description
  [description]
  (let [maximum-length 500]
    (cond (> (count description) maximum-length)
          (str (trim (subs description 0 (- maximum-length 3))) "...")
          :else description)))

(defn clear-label
  [label]
  (-> label
      delete-parenthesized-parts 
      trim))

(defn clear-tokens
  [tokens]
  ; Filter out roman numerals
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
        ;(.log js/console query)
        (>! sparql-results (<! (sparql-query-channel query))))
      (swap! app-state #(assoc % :loading? false)))
    sparql-results))

(defn sparql-autocomplete
  "Autocomplete from SPARQL endpoint.
  request: jQuery UI autocomplete's request object
  response: channel to put the response to"
  [request response]
  (let [fragment (lower-case (.-term request))
        sparql-results (sparql-results-channel)
        query (render-template autocomplete-template
                               :data {:classes (get-in config [:data :sparql :classes :default])
                                      :fragment fragment})
        sparql-query (sparql-query-channel query)]
    (go (let [results (map :label (<! (wrap-load sparql-query sparql-results)))]
          (response (clj->js results))))))

(defn wikipedia-autocomplete
  "Autocomplete from Wikipedia API.
  request: jQuery UI autocomplete's request object
  response: channel to put the response to"
  [request response]
  (let [fragment (lower-case (.-term request))
        query (http/jsonp "http://cs.wikipedia.org/w/api.php"
                          {:query-params {:action "opensearch"
                                          :format "json"
                                          :search fragment}})]
    (go (-> query <! :body second clj->js response))))

(defn load-gdocs-items
  "Load items from Google Spreadsheet's worksheet."
  [spreadsheet-id worksheet-id]
  (let [url (str "https://spreadsheets.google.com/feeds/list/"
                 spreadsheet-id "/" worksheet-id "/public/full")
        results (chan 1 (map (comp :entry :feed :body)))]
    (wrap-load (http/jsonp url {:query-params {:alt "json-in-script"}})
               results)))
