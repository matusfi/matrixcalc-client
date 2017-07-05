(ns matrixcalc-client.helpers)

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
