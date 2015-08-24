(ns db-quiz.model
  (:refer-clojure :exclude [replace])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [db-quiz.config :refer [config]]
            [db-quiz.state :refer [app-state]]
            [db-quiz.modals :as modals]
            [db-quiz.util :refer [number-of-fields]]
            [db-quiz.normalize :as normalize]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! >! chan pipe]]
            [clojure.string :refer [join lower-case replace split trim]]
            [cljsjs.mustache :as mustache]
            [reagent-modals.modals :as reagent-modals]))

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

(defn collapse-whitespace
  "Replace consecutive whitespace characters with a single space."
  [s]
  (replace s #"\s{2,}" " "))

(def clear-description
  "Cleaning of descriptions"
  (comp normalize/space-sentences collapse-whitespace))

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
      (let [query (render-template (<! query-channel) :data data)
            results (<! (sparql-query-channel sparql-endpoint query))]
        (if results
          (>! sparql-results results)
          (reagent-modals/modal! (modals/error-loading-data sparql-endpoint))))
      (swap! app-state #(assoc % :loading? false)))
    sparql-results))

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
  (let [worksheet-id "od6" ; FIXME: Is this value universally valid?
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
                (cond (< results-count number-of-fields)
                        (reagent-modals/modal! (modals/invalid-spreadsheet-rows results-count))
                      ; TODO: More validation rules
                      :else (>! results-chan (take number-of-fields (shuffle results))))))
          results-chan)
      (reagent-modals/modal! (modals/invalid-google-spreadsheet-url spreadsheet-url)))))

(defmulti merge-board-with-data
  "Merge data with questions into the initialized board."
  (fn [] (get-in @app-state [:options :labels])))

(defmethod merge-board-with-data :alphabetic
  [board data]
  (letfn [(field-by-initial [initial]
            (first (filter (fn [[k {:keys [text] :as v}]]
                             (= text initial))
                           board)))]
    (into {}
      (map (fn [{:keys [abbreviation] :as result}]
             (let [[k v] (field-by-initial (if (zero? (.indexOf abbreviation "Ch"))
                                             "Ch"
                                             (first abbreviation)))]
               [k (merge v result)]))
           data))))

(defmethod merge-board-with-data :default
  [board data]
  (into {}
        (map (fn [[k v] result] [k (merge v result)])
             board
             data)))

(defmulti load-board-data (fn [] (get-in @app-state [:options :data-source])))

(defmethod load-board-data :dbpedia
  [board callback]
  (let [{{:keys [difficulty labels selectors]} :options} @app-state
        endpoint "http://cs.dbpedia.org/sparql"
        count-file "sparql/cs_dbpedia_count.mustache"
        query-file (case labels
                         :numeric "sparql/cs_dbpedia.mustache"
                         :alphabetic "sparql/cs_dbpedia_az.mustache") 
        ; Offset is generated from the total count of available instances.
        ; The count is split into thirds, in which random offset is generated
        ; to select a random subset of the third. First third is easy difficulty,
        ; second third is normal difficulty, and the last third is hard difficulty.
        count-to-offset (fn [c]
                          (let [third (js/Math.floor (/ c 3))
                                offset (rand-int (- third number-of-fields))]
                            (case difficulty
                                  :easy offset
                                  :normal (+ third offset)
                                  :hard (+ (* third 2) offset))))
        count-query-channel (sparql-query endpoint
                                          count-file
                                          :data {:selectors selectors})
        query-channel-fn (fn [offset]
                           (sparql-query endpoint
                                         query-file
                                         :data {:selectors selectors
                                                :limit number-of-fields
                                                :offset offset}))]
    (go (let [count-result (js/parseInt (:count (first (<! count-query-channel))) 10)
              offset (count-to-offset count-result)
              results (map despoilerify (<! (query-channel-fn offset)))
              results-count (count results)]
          (if (= results-count number-of-fields)
            (do (swap! app-state #(assoc % :board (merge-board-with-data board results)))
                (callback))
            (reagent-modals/modal! (modals/invalid-number-of-results number-of-fields results-count)))))))

(defmethod load-board-data :gdrive
  [board callback]
  (let [spreadsheet-url (get-in @app-state [:options :doc])]
    (go (let [results (<! (load-gdocs-items spreadsheet-url))]
          (swap! app-state #(assoc % :board (merge-board-with-data board results)))
          (callback)))))
