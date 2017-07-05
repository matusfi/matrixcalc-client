(ns matrixcalc-client.matrix
  (:require [clojure.test.check.generators :as gen]))

(defn make-mtx
  [m]
  {:matrix m})

(defn pos->string
  [{r :row, c :col}]
  (str r "-" c))

(defn gen-mtx
  "Generate matrices using a generator with specified dimensions"
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
