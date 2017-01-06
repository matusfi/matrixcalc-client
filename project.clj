(defproject matrixcalc-client "0.1.0"
  :description "A generative test suite for the Matrix Calculator API implementations (http://docs.matrixcalc.apiary.io/)"
  :url "https://github.com/matusfi/matrixcalc-client"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [http-kit "2.1.19"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/test.check "0.9.0"]]
  :main ^:skip-aot matrixcalc-client.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
