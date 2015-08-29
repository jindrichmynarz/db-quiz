(ns db-quiz.model
  (:refer-clojure :exclude [replace])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [db-quiz.config :refer [config]]
            [db-quiz.state :refer [app-state]]
            [db-quiz.modals :as modals]
            [db-quiz.util :refer [number-of-fields render-template]]
            [db-quiz.normalize :as normalize]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! >! chan]]
            [clojure.string :refer [replace split]]
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
                        (when (and label description)
                          {:label label
                           :abbreviation (normalize/abbreviate label)
                           :description description}))
        raw-results-chan (wrap-load (http/jsonp url {:query-params {:alt "json-in-script"}})
                                (chan 1 (map (comp (partial map transform-row) :entry :feed :body))))
        results-chan (chan)]
    (if spreadsheet-id
      (do (go (let [results (<! raw-results-chan)
                    results-count (count results)]
                (cond (< results-count number-of-fields)
                        (reagent-modals/modal! (modals/invalid-spreadsheet-rows results-count))
                      (every? nil? results)
                        (reagent-modals/modal! modals/invalid-spreadsheet-columns)
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
    (go (if-let [count-result (:count (first (<! count-query-channel)))]
          (let [offset (count-to-offset (js/parseInt count-result 10))
                results (map normalize/despoilerify (<! (query-channel-fn offset)))
                results-count (count results)]
            (if (= results-count number-of-fields)
              (do (swap! app-state #(assoc % :board (merge-board-with-data board results)))
                  (callback))
              (reagent-modals/modal! (modals/invalid-number-of-results number-of-fields results-count))))
          (reagent-modals/modal! (modals/error-loading-data endpoint))))))

(defmethod load-board-data :gdrive
  [board callback]
  (let [spreadsheet-url (get-in @app-state [:options :doc])]
    (go (let [results (<! (load-gdocs-items spreadsheet-url))]
          (swap! app-state #(assoc % :board (merge-board-with-data board results)))
          (callback)))))
