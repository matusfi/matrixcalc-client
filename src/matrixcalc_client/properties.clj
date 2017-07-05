(ns matrixcalc-client.properties
  (:require [matrixcalc-client.matrix      :as mtx]
            [matrixcalc-client.http        :as http]
            [matrixcalc-client.helpers     :as help]
            [clojure.string                :as str]
            [clojure.test.check            :as tc]
            [clojure.test.check.properties :as prop]))

(def default-mtx-size 3)

(def can-get-all-fields-in-mtx
  (prop/for-all [matrix (mtx/gen-mtx mtx/gen-int default-mtx-size)]
                (not (nil? (mtx/at matrix (mtx/rand-pos default-mtx-size))))))

(def generators
  [mtx/gen-int mtx/gen-double])

(def operations
  [{:op "add" :fn +'}
   {:op "subtract" :fn -'}
   {:op "multiply" :fn *'}
   {:op "divide" :fn /}])

(def range-ops
  [{:op "sum" :fn +}
   {:op "product" :fn *}
   {:op "max" :fn max}
   {:op "min" :fn min}
   {:op "average" :fn (fn [& args]
                        (let [cnt (count args)
                              sum (apply + args)]
                          (if (zero? cnt) 0
                              (help/round (double (/ sum cnt))
                                          2))))}])

(defn make-prop
  [mtx-gen val-gen mtx-size test-fn]
  (prop/for-all [matrix (mtx-gen val-gen mtx-size)]
                      (test-fn matrix)))

(defn fn-over-matrix
  ([matrix op]
   (apply (:fn op) (flatten matrix)))
  ([matrix op range]
   (apply (:fn op) (mtx/get-range matrix range)))
  ([matrix op pos-a pos-b]
   (let [val-a (mtx/at matrix pos-a)
         val-b (mtx/at matrix pos-b)
         fun   (:fn op)]
     (fun val-a val-b))))

(defn log-fail
  ([expected actual url matrix op]
   (println
    (str "FAIL: expected '" expected "' but was '" actual "', "
         "[" url "], "
         "op: " (:op op) ", "
         "matrix: " matrix)))
  ([expected actual url matrix op range]
   (println
    (str  "FAIL: expected '" expected "' but was '" actual "', "
          "[" url "], "
          "op: "(:op op) ", "
          "range: " (mtx/pos->string range) ", "
          "matrix: " matrix)))
  ([expected actual url matrix op pos-a pos-b]
   (println
    (str "FAIL: expected '" expected "' but was '" actual "', "
         "[" url "], "
         "op: "(:op op) ", "
         "pos-a: " (mtx/pos->string pos-a) ", "
         "pos-b: " (mtx/pos->string pos-b) ", "
         "matrix: " matrix))))

(defn test-over-url
  [base-url op]
  (fn [matrix]
    (let [size  (count matrix)
          pos-a (mtx/rand-pos size)
          pos-b (mtx/rand-pos size)
          expected (fn-over-matrix matrix op pos-a pos-b)
          url   (http/make-url base-url op pos-a pos-b)
          res-f (http/make-request url (mtx/make-mtx matrix))
          actual   (http/get-result res-f)
          pass? (help/equals expected actual)]
      (when-not pass?
        (log-fail expected actual url matrix op pos-a pos-b))
      pass?)))

(defn test-url-with-zero
  [base-url]
  (fn [matrix]
    (let [size              (count matrix)
          pos-a             (mtx/rand-pos size)
          pos-b             (mtx/rand-pos size)
          matrix-with-zero  (mtx/at matrix pos-b 0)
          url               (http/make-url base-url
                                      {:op "divide" :fn /}
                                      pos-a pos-b)
          res-f (http/make-request url (mtx/make-mtx matrix-with-zero))
          error (http/get-error res-f)
          pass? (not (nil? error))]
      (when-not pass?
        (println "FAIL: No error reported when attempting division by zero. " @res-f))
      pass?)))

(defn test-ranged
  [base-url op]
  (fn [matrix]
    (let [size  (count matrix)
          range (mtx/rand-range size)
          expected (fn-over-matrix matrix op range)
          url   (http/make-url base-url op range)
          res-f (http/make-request url (mtx/make-mtx matrix))
          actual (http/get-result res-f)
          pass? (help/equals expected actual)]
      (when-not pass?
        (log-fail expected actual url matrix op range))
      pass?)))

(defn test-on-whole-matrix
  [base-url op]
  (fn [matrix]
    (let [expected (fn-over-matrix matrix op)
          url       (http/make-url base-url op)
          res-f     (http/make-request url (mtx/make-mtx matrix))
          actual    (http/get-result res-f)
          pass?     (help/equals expected actual)]
      (when-not pass?
        (log-fail expected actual url matrix op))
      pass?)))

(defn basic-props
  [url]
  (let [tests (map #(test-over-url url %) operations)
        props (map #(make-prop mtx/gen-non-zero-mtx mtx/gen-int default-mtx-size %) tests)]
    {:desc  "Basic Properties"
     :ops   (map :op operations)
     :props props}))

(defn div-by-zero-prop
  [url]
  (let [prop (make-prop mtx/gen-non-zero-mtx
                        mtx/gen-int
                        default-mtx-size
                        (test-url-with-zero url))]
    {:desc  "Division-by-zero"
     :ops   (list "divide")
     :props (list prop)}))

(defn range-ops-props
  [url]
  (let [tests (map #(test-ranged url %) range-ops)
        props (map #(make-prop mtx/gen-mtx mtx/gen-int default-mtx-size %) tests)]
    {:desc  "Ranged operations"
     :ops   (map :op range-ops)
     :props props}))

(defn whole-mtx-props
  [url]
  (let [tests (map #(test-on-whole-matrix url %) range-ops)
        props (map #(make-prop mtx/gen-mtx mtx/gen-int default-mtx-size %) tests)]
    {:desc  "Whole matrix operations"
     :ops   (map :op range-ops)
     :props props}))

(defn test-props
  [test-size properties]
  (for [prop properties]
    (dorun
     (do
       (println "Testing" (:desc prop) "=>" (:ops prop))
       (for [p (:props prop)]
         (tc/quick-check test-size p))))))
