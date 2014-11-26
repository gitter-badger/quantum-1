(ns quantum.core.util.test
  (:require
  	[quantum.core.string :as str]
  	[quantum.core.collections :as coll :refer [merge+]])
  (:gen-class))

(set! *warn-on-reflection* true)

(defn rand-maps [ct]
  (for [n (range 0 ct)]
    (hash-map
      (keyword (str/rand-str 30))
      (str/rand-str 5))))
(defn rand-map [ct]
  (apply merge+ (rand-maps ct)))