(ns matrixcalc-client.core
  (:require [org.httpkit.client :as http]
            [clojure.data.json  :as json]
            [clojure.string     :as str]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop])
  (:gen-class))

(def ^:dynamic *url*
  "http://matrixcalc.demecko.com/api/")

(defn make-mtx
  [m]
  {:matrix m})

(defn pos->string
  [{r :row, c :col}]
  (str r "-" c))

(defn make-url
  "Piece together an URL for the API usage"
  ([base-url op]
   (str base-url (:op op)))
  ([base-url op range]
   (str (make-url base-url op)
        "?range=" (pos->string range)))
  ([base-url op pos-a pos-b]
   (str (make-url base-url op)
        "/"
        (pos->string pos-a)
        "/"
        (pos->string pos-b))))

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

(defn get-error
  [res-future]
  (-> @res-future
      :body
      json/read-str
      (get "error")))

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
  [size]
  (let [r (inc (rand-int size))
        c (inc (rand-int size))]
    {:row r
     :col c}))

(defn rand-range
  [size]
  (let [where (if (zero? (rand-int 2)) :row :col)
        pos   (rand-pos size)]
    (assoc pos where \x)))

(defn at
  "Get/set a value at specified position in the matrix"
  ([mtx pos]
   (let [row (dec (:row pos))
         col (dec (:col pos))]
     (-> mtx
         (nth row nil)
         (nth col nil))))
  ([mtx pos val]
   (let [row (dec (:row pos))
         col (dec (:col pos))]
     (assoc-in mtx [row col] val))))

(defn get-range
  [mtx range]
  (let [where (if (= (:row range) \x) :col :row)
        pos   (dec (if (= where :row)
                     (:row range)
                     (:col range)))]
    (if (= where :row)
      (nth mtx pos)
      (map #(nth % pos) mtx))))

(defn round
  "Round down a double to the given precision (number of significant digits)"
  [^double d ^long precision]
  (let [factor (Math/pow 10 precision)]
    (/ (Math/round (* d factor)) factor)))

(defn equals
  [a b]
  (if (or (= java.lang.Double (type a))
          (= java.lang.Double (type b)))
    (let [precision 0.02]
      (<= (Math/abs (- a b)) precision))
    (== a b)))

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

(def range-ops
  [{:op "sum" :fn +}
   {:op "product" :fn *}
   {:op "max" :fn max}
   {:op "min" :fn min}
   {:op "average" :fn (fn [& args]
                        (let [cnt (count args)
                              sum (apply + args)]
                          (if (zero? cnt) 0
                              (round (double (/ sum cnt))
                                     2))))}])

(defn make-prop
  [mtx-gen val-gen mtx-size test-fn]
  (prop/for-all [mtx (mtx-gen val-gen mtx-size)]
                      (test-fn mtx)))

(defn fn-over-mtx
  ([mtx op]
   (apply (:fn op) (flatten mtx)))
  ([mtx op range]
   (apply (:fn op) (get-range mtx range)))
  ([mtx op pos-a pos-b]
   (let [val-a (at mtx pos-a)
         val-b (at mtx pos-b)
         fun   (:fn op)]
     (fun val-a val-b))))

(defn log-fail
  ([expected actual url mtx op]
   (println
    (str "FAIL: expected '" expected "' but was '" actual "', "
         "[" url "], "
         "op: " (:op op) ", "
         "mtx: " mtx)))
  ([expected actual url mtx op range]
   (println
    (str  "FAIL: expected '" expected "' but was '" actual "', "
          "[" url "], "
          "op: "(:op op) ", "
          "range: " (pos->string range) ", "
          "mtx: " mtx)))
  ([expected actual url mtx op pos-a pos-b]
   (println
    (str "FAIL: expected '" expected "' but was '" actual "', "
         "[" url "], "
         "op: "(:op op) ", "
         "pos-a: " (pos->string pos-a) ", "
         "pos-b: " (pos->string pos-b) ", "
         "mtx: " mtx))))

(defn test-over-url
  [base-url op]
  (fn [mtx]
    (let [size  (count mtx)
          pos-a (rand-pos size)
          pos-b (rand-pos size)
          expected (fn-over-mtx mtx op pos-a pos-b)
          url   (make-url base-url op pos-a pos-b)
          res-f (make-request url (make-mtx mtx))
          actual   (get-result res-f)
          pass? (equals expected actual)]
      (when-not pass?
        (log-fail expected actual url mtx op pos-a pos-b))
      pass?)))

(defn test-url-with-zero
  [base-url]
  (fn [mtx]
    (let [size  (count mtx)
          pos-a (rand-pos size)
          pos-b (rand-pos size)
          mtx-with-zero (at mtx pos-b 0)
          url   (make-url base-url
                          {:op "divide" :fn /}
                          pos-a pos-b)
          res-f (make-request url (make-mtx mtx-with-zero))
          error (get-error res-f)
          pass? (not (nil? error))]
      (when-not pass?
        (println "FAIL: No error reported when attempting division by zero. " @res-f))
      pass?)))

(defn test-ranged
  [base-url op]
  (fn [mtx]
    (let [size  (count mtx)
          range (rand-range size)
          expected (fn-over-mtx mtx op range)
          url   (make-url base-url op range)
          res-f (make-request url (make-mtx mtx))
          actual (get-result res-f)
          pass? (equals expected actual)]
      (when-not pass?
        (log-fail expected actual url mtx op range))
      pass?)))

(defn test-on-whole-mtx
  [base-url op]
  (fn [mtx]
    (let [expected (fn-over-mtx mtx op)
          url       (make-url base-url op)
          res-f     (make-request url (make-mtx mtx))
          actual    (get-result res-f)
          pass?     (equals expected actual)]
      (when-not pass?
        (log-fail expected actual url mtx op))
      pass?)))

(defn basic-props
  []
  (let [tests (map #(test-over-url *url* %) operations)
        props (map #(make-prop gen-non-zero-mtx gen-int default-mtx-size %) tests)]
    {:desc  "Basic Properties"
     :ops   (map :op operations)
     :props props}))

(defn div-by-zero-prop
  []
  (let [prop (make-prop gen-non-zero-mtx
                        gen-int
                        default-mtx-size
                        (test-url-with-zero *url*))]
    {:desc  "Division-by-zero"
     :ops   (list "divide")
     :props (list prop)}))

(defn range-ops-props
  []
  (let [tests (map #(test-ranged *url* %) range-ops)
        props (map #(make-prop gen-mtx gen-int default-mtx-size %) tests)]
    {:desc  "Ranged operations"
     :ops   (map :op range-ops)
     :props props}))

(defn whole-mtx-props
  []
  (let [tests (map #(test-on-whole-mtx *url* %) range-ops)
        props (map #(make-prop gen-mtx gen-int default-mtx-size %) tests)]
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

(defn -main
  [& args]
  (assert (not (zero? (count args))))
  (binding [*url* (first args)]
    (let [all-props (vector (basic-props)
                            (div-by-zero-prop)
                            (range-ops-props) 
                            (whole-mtx-props))
          test-size (if (nil? (second args)) 5 (second args))]
      (dorun (test-props test-size all-props)))))
