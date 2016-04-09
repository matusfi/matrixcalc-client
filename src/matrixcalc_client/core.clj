(ns matrixcalc-client.core
  (:use midje.sweet)
  (:require [org.httpkit.client :as http]
            [clojure.data.json  :as json]
            [clojure.string     :as str]
            [clojure.test :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop])
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(def url
  "http://matrixcalc.demecko.com/api/")

(defn make-mtx
  [m]
  {:matrix m})

(defn make-request
  [url body]
  (http/request {:url url,
                 :method :get
                 :headers {"Content-Type" "application/json"}
                 :body (json/write-str body)}))

(defn get-result
  [res-future]
  (-> @res-future
      :body
      json/read-str
      (get "result")))

(defn gen-mtx
  "Generate matrices of using a generator with specified dimensions"
  [generator size]
  (gen/vector
   (gen/vector generator size)
   size))

(defn gen-non-zero-mtx
  "Generate matrices that contain no zeroes"
  [generator size]
  (gen/vector
   (gen/such-that #(not-any? zero? %) (gen/vector generator size))
   size))

(def gen-int
  gen/large-integer)

(def gen-double
  (gen/double* {:infinite? false
                :NaN?      false}))

(defn rand-pos
  [range]
  (let [r (inc (rand-int range))
        c (inc (rand-int range))]
    {:row r
     :col c}))

(defn pos-to-string
  [{r :row, c :col}]
  (str r "-" c))

(defn at
  "Get a value at specified position in the matrix"
  [mtx pos]
  (let [row (dec (:row pos))
        col (dec (:col pos))]
    (-> mtx
        (nth row nil)
        (nth col nil))))


;;; Properties

(def default-mtx-size 3)

(def can-get-all-fields-in-mtx
  (prop/for-all [mtx (gen-mtx gen-int default-mtx-size)]
                (not (nil? (at mtx (rand-pos default-mtx-size))))))

(def generators
  [gen-int gen-double])

(def operations
  [{:op "add" :fn +'}
   {:op "subtract" :fn -'}
   {:op "multiply" :fn *'}
   {:op "divide" :fn /}])

(defn make-prop
  [mtx-gen val-gen mtx-size test-fn]
  (prop/for-all [mtx (mtx-gen val-gen mtx-size)]
                (test-fn mtx)))

(defn make-url
  [base-url op pos-a pos-b]
  (str base-url
       (:op op)
       "/"
       (pos-to-string pos-a)
       "/"
       (pos-to-string pos-b)))

(defn fn-over-mtx
  ([mtx op pos-a pos-b]
   (let [val-a (at mtx pos-a)
         val-b (at mtx pos-b)
         fun   (:fn op)]
     (fun val-a val-b))))

(defn test-over-url
  [base-url op]
  (fn [mtx]
    (let [size  (count mtx)
          pos-a (rand-pos size)
          pos-b (rand-pos size)
          res-local (fn-over-mtx mtx op pos-a pos-b)
          url   (make-url base-url op pos-a pos-b)
          res-f (make-request url (make-mtx mtx))]
      (= res-local (get-result res-f)))))

(def basic-props
  (let [tests (map #(test-over-url url %) operations)]
    ))

(defn test-props
  [test-size properties]
  (for [prop properties]
    (tc/quick-check test-size prop)))


;;; Get Random Matrix
;;; Choose rand positions (pos-a & pos-b)
;;; Perform a function over the Mtx locally => get the local-result

;;; Construct the operation URL
;;; Send request => get the remote-result
;;; Results should be the equal


