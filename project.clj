(defproject db-quiz "0.8.0-SNAPSHOT"
  :description "A database-backed take on the game of AZ-kvíz"
  :url "http://github.com/jindrichmynarz/db-quiz"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj" "src/cljs"]

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/clojurescript "1.7.122" :scope "provided"]
                 [ring-server "0.4.0"]
                 [cljsjs/mustache "1.1.0-0"]
                 [reagent "0.5.1"]
                 [reagent-utils "0.1.5"]
                 [ring "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [prone "0.8.2"]
                 [compojure "1.4.0"]
                 [hiccup "1.0.5"]
                 [environ "1.0.1"]
                 [clj-tagsoup/clj-tagsoup "0.3.0" :exclusions [org.clojure/clojure]]
                 [cljs-http "0.1.37"]
                 [secretary "1.2.3"]
                 [clj-fuzzy "0.3.1"]
                 [org.clojars.frozenlock/reagent-modals "0.2.3"]
                 [com.taoensso/tower "3.1.0-beta3"]]

  :plugins [[lein-environ "1.0.0"]
            [lein-asset-minifier "0.2.2"]]

  :ring {:handler db-quiz.handler/app
         :uberwar-name "db-quiz.war"}

  :min-lein-version "2.5.0"

  :uberjar-name "db-quiz.jar"

  :main db-quiz.server

  :clean-targets ^{:protect false} [:target-path
                                    [:cljsbuild :builds :app :compiler :output-dir]
                                    [:cljsbuild :builds :app :compiler :output-to]]

  :minify-assets
  {:assets
    {"resources/public/css/site.min.css" "resources/public/css/site.css"}}

  :cljsbuild {:builds {:app {:source-paths ["src/cljs"]
                             :compiler {:output-to     "resources/public/js/app.js"
                                        :output-dir    "resources/public/js/out"
                                        :asset-path   "js/out"
                                        :optimizations :none
                                        :pretty-print  true}}}}

  :profiles {:dev {:repl-options {:init-ns db-quiz.repl
                                  :nrepl-middleware []}

                   :dependencies [[ring-mock "0.1.5"]
                                  [ring/ring-devel "1.4.0"]
                                  [leiningen-core "2.5.3"]
                                  [lein-figwheel "0.4.0"]
                                  [org.clojure/tools.nrepl "0.2.11"]
                                  [pjstadig/humane-test-output "0.7.0"]
                                  [org.clojure/test.check "0.8.2"]]

                   :source-paths ["env/dev/clj"]
                   :plugins [[lein-doo "0.1.5-SNAPSHOT"]
                             [lein-figwheel "0.4.0"]
                             [lein-cljsbuild "1.1.0"]]

                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]

                   :figwheel {:http-server-root "public"
                              :server-port 3449
                              :nrepl-port 7002
                              :css-dirs ["resources/public/css"]
                              :ring-handler db-quiz.handler/app}

                   :env {:dev true}

                   :cljsbuild {:builds {:app {:source-paths ["env/dev/cljs"]
                                              :compiler {:main "db-quiz.dev"
                                                         :optimizations :none
                                                         :source-map true}}
                                        :test {:source-paths ["src/cljs"  "test/cljs"]
                                               :compiler {:output-to "target/test.js"
                                                          :main db-quiz.runner
                                                          :optimizations :whitespace
                                                          :pretty-print true}}}}}

             :uberjar {:hooks [leiningen.cljsbuild minify-assets.plugin/hooks]
                       :env {:production true}
                       :aot :all
                       :omit-source true
                       :cljsbuild {:jar true
                                   :builds {:app
                                             {:source-paths ["env/prod/cljs"]
                                              :compiler
                                              {:externs ["externs/bootstrap.js"]
                                               :optimizations :advanced
                                               :pretty-print false}}}}}})
