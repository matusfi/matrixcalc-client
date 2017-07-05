(ns matrixcalc-client.core
  (:require [matrixcalc-client.properties :as props]
            [matrixcalc-client.http       :as http])
  (:gen-class))

(def default-url
  "http://private-f056ad-matrixcalc.apiary-mock.com")

(defn -main
  [& args]
  (let [url       (if-not (zero? (count args))
                          (http/fix-url (first args))
                          default-url)
        all-props (vector (props/basic-props url)
                          (props/div-by-zero-prop url)
                          (props/range-ops-props url)
                          (props/whole-mtx-props url))
        test-size (if (nil? (second args)) 5 (read-string (second args)))]
    (println "Tested URL: " url)
    (dorun (props/test-props test-size all-props))))
