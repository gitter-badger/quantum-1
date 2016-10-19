(ns
  ^{:doc
      "Various collections functions.

       Includes better versions of the following than clojure.core:

       for, doseq, repeat, repeatedly, range, merge,
       count, vec, reduce, into, first, second, rest,
       last, butlast, get, pop, peek ...

       and more.

       Many of them are aliased from other namespaces like
       quantum.core.collections.core, or quantum.core.reducers."
    :attribution "Alex Gunnarson"}
  quantum.core.collections.map-filter
  (:refer-clojure :exclude
    [for doseq reduce
     contains?
     repeat repeatedly
     interpose
     range
     take take-while
     drop  drop-while
     subseq
     key val
     merge sorted-map sorted-map-by
     into
     count
     empty empty?
     split-at
     first second rest last butlast get pop peek
     select-keys
     zipmap
     reverse
     conj
     conj! assoc! dissoc! disj!
     boolean?])
  (:require
    [#?(:clj  clojure.core
        :cljs cljs.core   )        :as core]
    [quantum.core.data.map         :as map
      :refer [map-entry]]
    [quantum.core.collections.core :as coll
      :refer        [key val conj
                     #?@(:clj [first rest conj! lasti contains?])]
      :refer-macros [          first rest conj! lasti contains?]]
    [quantum.core.fn               :as fn
      :refer        [juxt-kv
                     #?@(:clj [<- rcomp defcurried])]
      :refer-macros [          <- rcomp defcurried]]
    [quantum.core.macros           :as macros
      :refer        [#?@(:clj [defnt])]
      :refer-macros [          defnt]]
    [quantum.core.reducers         :as red
      :refer        [#?@(:clj [reduce])]
      :refer-macros [          reduce]]
    [quantum.core.type             :as type]
    [quantum.core.loops            :as loops
      :refer        [#?@(:clj [reducei doseqi lfor])]
      :refer-macros [          reducei doseqi lfor]]
    [quantum.core.vars             :as var
      :refer        [#?@(:clj [defalias])]
      :refer-macros [          defalias]]))

(defcurried each ; like doseq
  "Applies f to each item in coll, returns nil"
  {:attribution "transduce.reducers"}
  [f coll]
  (reduce (fn [_ x] (f x) nil) nil coll))

; ============================ MAP ============================ ;

(defalias lmap         core/map        )
(defalias map+         red/map+        )
(defalias map-indexed+ red/map-indexed+)

(defn map-keys+ [f coll] (->> coll (map+ (juxt-kv f identity))))
(defn map-vals+ [f coll] (->> coll (map+ (juxt-kv identity f))))

; ============================ FILTER ============================ ;

(defalias filter+     red/filter+    )
(defalias lfilter     filter         )

(defn ffilter
  "Returns the first result of a |filter| operation.
   Uses lazy |filter| so as to do it in the fastest possible way."
   [filter-fn coll]
   (->> coll (filter filter-fn) first))

(defn ffilter+
  {:todo ["Use a delayed reduction as the base!"]}
  [pred coll]
  (reduce
    (fn [ret elem-n]
      (when (pred elem-n)
        (reduced elem-n)))
    nil
    coll))

(defn ffilteri
  {:todo ["Use a delayed reduction as the base!" "Allow parallelization"]
   :in   '[(fn-eq? "4") ["a" "d" "t" "4" "10"]]
   :out  [3 "4"]
   :out-type 'MapEntry}
  [pred coll]
  (reducei
    (fn [ret elem-n index-n]
      (if (pred elem-n)
          (reduced (map-entry index-n elem-n))
          ret))
    (map-entry nil nil)
    coll))

(defn filteri
  {:todo ["Use reducers"]}
  [pred coll]
  (if (type/should-transientize? coll)
      (persistent!
        (loops/reducei
          (fn [ret elem-n n]
            (if (pred elem-n)
                (conj! ret (map-entry n elem-n))
                ret))
          (transient [])
          coll))
      (loops/reducei
        (fn [ret elem-n n]
          (if (pred elem-n)
              (conj ret (map-entry n elem-n))
              ret))
        []
        coll)))

(defnt ^clojure.lang.MapEntry last-filteri*
  {:todo ["Use a delayed reduction as the base!" "Allow parallelization"]
   :in   '[["a" "d" "t" "4" "4" "10"] (fn-eq? "4")]
   :out  [4 "4"]}
  ([^indexed? coll pred]
    (->> coll rseq (ffilteri pred)
         (<- update 0 (partial - (lasti coll)))))
  ([coll pred]
    (loops/reducei
      (fn [ret elem-n index-n]
        (if (pred elem-n)
            (map-entry index-n elem-n)
            ret))
      (map-entry nil nil)
      coll)))

#?(:clj  (definline last-filteri [pred coll] `(last-filteri* ~coll ~pred))
   :cljs (defn      last-filteri [pred coll]  (last-filteri*  coll  pred)))

(defalias lremove core/remove)
(defalias remove+ red/remove+)
;___________________________________________________________________________________________________________________________________
;=================================================={  FILTER + REMOVE + KEEP  }=====================================================
;=================================================={                          }=====================================================
(defn filter-keys+ [pred coll] (->> coll (filter+ (rcomp key pred))))
(defn remove-keys+ [pred coll] (->> coll (remove+ (rcomp key pred))))
(defn filter-vals+ [pred coll] (->> coll (filter+ (rcomp val pred))))
(defn remove-vals+ [pred coll] (->> coll (remove+ (rcomp val pred))))

; Distinct can be seen as sort of a filter

(defn ldistinct-by
  "Returns a lazy sequence of the elements of coll, removing any elements that
  return duplicate values when passed to a function f."
  {:attribution "medley.core"}
  [f coll]
  (let [step (fn step [xs seen]
               (lazy-seq
                ((fn [[x :as xs] seen]
                   (when-let [s (seq xs)]
                     (let [fx (f x)]
                       (if (contains? seen fx)
                         (recur (rest s) seen)
                         (cons x (step (rest s) (conj seen fx)))))))
                 xs seen)))]
    (step coll #{})))

#?(:clj
(defn ldistinct-by-java
  "Returns elements of coll which return unique
   values according to f. If multiple elements of coll return the same
   value under f, the first is returned"
  {:attribution "prismatic.plumbing"
   :performance "Faster than |distinct-by|"}
  [f coll]
  (let [s (java.util.HashSet.)] ; instead of #{}
    (lfor [x coll
           :let [id (f x)]
           :when (not (.contains s id))]
     (do (.add s id)
         x)))))
