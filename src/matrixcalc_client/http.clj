(ns matrixcalc-client.http
  (:require [org.httpkit.client       :as http]
            [clojure.data.json        :as json]
            [clojure.string           :as str]
            [matrixcalc-client.matrix :as mtx]))

(defn fix-url
  [unsafe-url]
  (let [trimmed-url (str/trim unsafe-url)]
    (if (= \/ (last trimmed-url))
      trimmed-url
      (str trimmed-url \/))))

(defn make-url
  "Piece together an URL for the API usage"
  ([base-url op]
   (str base-url (:op op)))
  ([base-url op range]
   (str (make-url base-url op)
        "?range=" (mtx/pos->string range)))
  ([base-url op pos-a pos-b]
   (str (make-url base-url op)
        "/"
        (mtx/pos->string pos-a)
        "/"
        (mtx/pos->string pos-b))))

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
