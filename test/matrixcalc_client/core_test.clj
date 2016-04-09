(ns matrixcalc-client.core-test
  (:require [clojure.test :refer :all]
            [matrixcalc-client.core :refer :all]))

(def url
  "http://matrixcalc.demecko.com/api/add/1-1/2-2")

(def mtx
  {:matrix [[1 2 3]
            [4 5 6]
            [7 8 9]]})

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))
