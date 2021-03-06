 (ns
  ^{:doc "Retakes on core collections functions like first, rest,
          get, nth, last, index-of, etc.

          Also includes innovative functions like getr, etc."}
  quantum.core.collections.core
  (:refer-clojure :exclude
    [vector hash-map rest count first second butlast last aget get nth pop peek
     conj! conj assoc assoc! dissoc dissoc! disj! contains? key val reverse subseq
     empty? empty class reduce swap! reset!
     #?@(:cljs [array])])
  (:require [clojure.core                   :as core]
    #?(:clj [seqspert.vector                            ])
    #?(:clj [clojure.core.async             :as casync])
            [quantum.core.log               :as log]
            [quantum.core.collections.base
              :refer [kmap nempty? nnil?]]
            [quantum.core.convert.primitive :as pconvert
              :refer [->boolean
                      ->byte
              #?(:clj ->char)
                      ->short
                      ->int
                      ->long
              #?(:clj ->float)
                      ->double
            #?@(:clj [->byte*
                      ->char*
                      ->short*
                      ->int*
                      ->long*
                      ->float*
                      ->double*])]]
            [quantum.core.data.vector       :as vec
              :refer [catvec svector subsvec]]
            [quantum.core.error             :as err
              :refer [->ex TODO]]
            [quantum.core.fn                :as fn
              :refer [fn1 fn&2 rfn]]
            [quantum.core.logic             :as logic
              :refer [fn= whenc whenf ifn1]]
            [quantum.core.collections.logic
              :refer [seq-or]]
            [quantum.core.macros            :as macros
              :refer [defnt]]
            [quantum.core.macros.optimization
              :refer [identity*]]
            [quantum.core.reducers          :as red
              :refer [drop+ take+ reduce
                      #?@(:clj [dropr+ taker+])]]
            [quantum.core.type              :as type
              :refer [class pattern?]]
            [quantum.core.type.defs         :as tdef]
            [quantum.core.vars              :as var
              :refer [defalias def-]])
  (:require-macros
    [quantum.core.collections.core
      :refer [assoc]])
 #?(:clj (:import
           quantum.core.data.Array
           [clojure.lang IAtom]
           [java.util List Collection Map]
           [java.util.concurrent.atomic AtomicReference AtomicBoolean AtomicInteger AtomicLong])))

; FastUtil is the best
; http://java-performance.info/hashmap-overview-jdk-fastutil-goldman-sachs-hppc-koloboke-trove-january-2015/

; TODO notify of changes to:
; https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/RT.java
; https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/Util.java
; https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/Numbers.java
; TODO Queues need support

; TODO implement all these using wagjo/data-cljs
; slice [o start end] - like clojure.core/subvec
; slice-from [o start] - like slice, but until the end of o
; slice-to [o end] - like slice, but from the beginning of o
; split-at [o index] - clojure.core/split-at
; cat [o o2] - eager variant of clojure.core/concat
; splice [o index n val] - fast remove and insert in one go
; splice-arr [o index n val-arr] - fast remove and insert in one go
; insert-before [o index val] - insert one item inside coll
; insert-before-arr [o index val] - insert array of items inside coll
; remove-at [o index] - remove one itfem from index pos
; remove-n [o index n] - remove n items starting at index pos
; rip [o index] - rips coll and returns [pre-coll item-at suf-coll]
; sew [pre-coll item-arr suf-coll] - opposite of rip, but with arr

; mape-indexed [f o] - eager version of clojure.core/map-indexed
; reduce-reverse [f init o] - like reduce but in reverse order
; reduce2-reverse [f o] - like reduce but in reverse order
; reduce-kv-reverse [f init o] - like reduce-kv but in reverse order
; reduce2-kv-reverse [f o] - like reduce-kv but in reverse order

; Arbitrary.
; TODO test this on every permutation for inflection point.
(def- parallelism-threshold 10000)

; https://github.com/JulesGosnell/seqspert
; Very useful sequence and data structure info.

;___________________________________________________________________________________________________________________________________
;=================================================={        EQUIVALENCE       }=====================================================
;=================================================={       =, identical?      }=====================================================
; (defnt ^boolean identical?
;   [^Object k1, ^Object k2]
;   (clojure.lang.RT/identical k1 k2))

; static public boolean pcequiv(Object k1, Object k2){
;   if(k1 instanceof IPersistentCollection)
;     return ((IPersistentCollection)k1).equiv(k2);
;   return ((IPersistentCollection)k2).equiv(k1);
; }

; static public boolean equals(Object k1, Object k2){
;   if(k1 == k2)
;     return true;
;   return k1 != null && k1.equals(k2);
; }

; static public boolean equiv(Object k1, Object k2){
;   if(k1 == k2)
;     return true;
;   if(k1 != null)
;     {
;     if(k1 instanceof Number && k2 instanceof Number)
;       return Numbers.equal((Number)k1, (Number)k2);
;     else if(k1 instanceof IPersistentCollection || k2 instanceof IPersistentCollection)
;       return pcequiv(k1,k2);
;     return k1.equals(k2);
;     }
;   return false;
; }

; equivNull   : boolean equiv(Object k1, Object k2) return k2 == null
; equivEquals : boolean equiv(Object k1, Object k2) return k1.equals(k2)
; equivNumber : boolean equiv(Object k1, Object k2)
;             if(k2 instanceof Number)
;                 return Numbers.equal((Number) k1, (Number) k2);
;             return false

; equivColl : boolean equiv(Object k1, Object k2)
;             if(k1 instanceof IPersistentCollection || k2 instanceof IPersistentCollection)
;                 return pcequiv(k1, k2);
;             return k1.equals(k2);

; ; equivPred:
; ;     nil             : equivNull
; ;     Number          : equivNumber
; ;     String, Symbol  : equivEquals
; ;     Collection, Map : equivColl
; ;     :else           : equivEquals

; (defnt equiv ^boolean
;   ([^Object                a #{long double boolean} b] (clojure.lang.RT/equiv a b))
;   ([#{long double boolean} a ^Object                b] (clojure.lang.RT/equiv a b))
;   ([#{long double boolean} a #{long double boolean} b] (clojure.lang.RT/equiv a b))
;   ([^char                  a ^char                  b] (clojure.lang.RT/equiv a b))

;   )

;___________________________________________________________________________________________________________________________________
;=================================================={         RETRIEVAL        }=====================================================
;=================================================={     get, first, rest     }=====================================================
#_(defnt ^"Object[]" ->array
  {:source "clojure.lang.RT.toArray"}
  ([^"Object[]" x] x)
  ([^Collection x] (.toArray x))
  ([^Iterable   x]
    (let [ret (ArrayList. x)]
      (doseq [elem x]
        (.add ret elem))
      (.toArray ret)))
  ([^Map    x] (-> x (.entrySet) (.toArray)))
  ([^String x]

    (let [chars (-> x (.toCharArray))
          ret   (object-array (count chars))]

    ;  for(int i = 0 i < chars.length i++)
    ;   ret[i] = chars[i]
    ; return ret;
     ))
  ([^array-1d? x]
    (let [s   (seq x)
          ret (object-array (count s))]
    ;   for(int i = 0; i < ret.length; i++, s = s.next())
    ;   ret[i] = s.first();
    ; return ret
    ))
  ([^Object x]
    (if (nil? x)
        clojure.lang.RT/EMPTY_ARRAY
        (throw (Util/runtimeException (str "Unable to convert: " (.getClass x) " to Object[]"))))))

(declare array)

(defnt count
  {:todo #{"incorporate clojure.lang.RT/count"
           "Ensure that lazy-seqs aren't realized/consumed?"
           "handle persistent maps"}}
           (^long [^array?     x] (#?(:clj Array/count :cljs .-length) x))
           (^long [^string?    x] (#?(:clj .length :cljs .-length  ) x))
           (^long [^!string?   x] (#?(:clj .length :cljs .getLength) x))
           (^long [^keyword?   x] (count ^String (name x)))
           (^long [^m2m-chan?  x] (count (#?(:clj .buf :cljs .-buf) x)))
           (^long [^+vec?      x] (#?(:clj .count :cljs core/count) x))
  #?(:clj  (^long [^Collection x] (.size x)))
           (^long [            x] (core/count x))
           (^long [^reducer?   x] (red/reduce-count x)))

(defnt empty?
          ([#{array? ; TODO anything that `count` accepts
              string? !string? keyword? chan?
              +vec?} x] (zero? (count x)))
  #?(:clj ([#{Collection Map} x] (.isEmpty x)))
          ([            x] (core/empty? x)  ))

(defnt empty
  {:todo #{"Most of this should be in some static map somewhere for efficiency"
           "implement core/empty"}}
           (^boolean [^boolean?  x] false         )
  #?(:clj  (^char    [^char?     x] (->char   0)  ))
  #?(:clj  (^byte    [^byte?     x] (->byte   0)  ))
  #?(:clj  (^short   [^short?    x] (->short  0)  ))
  #?(:clj  (^int     [^int?      x] (->int    0)  ))
  #?(:clj  (^long    [^long?     x] (->long   0)  ))
  #?(:clj  (^float   [^float?    x] (->float  0)  ))
  #?(:clj  (^double  [^double?   x] (->double 0)  ))
  #?(:cljs (         [^pnum?     x] 0             ))
           (^String  [^string?   x] ""            )
           (         [           x] (core/empty x)))

(defnt #?(:clj  ^long lasti
          :cljs       lasti)
  "Last index of a coll."
  [coll] (unchecked-dec (count coll)))

#?(:clj
(defnt array-of-type
  (^first [^short-array?   obj ^nat-long? n] (short-array   n))
  (^first [^long-array?    obj ^nat-long? n] (long-array    n))
  (^first [^float-array?   obj ^nat-long? n] (float-array   n))
  (^first [^int-array?     obj ^nat-long? n] (int-array     n))
  (^first [^double-array?  obj ^nat-long? n] (double-array  n))
  (^first [^boolean-array? obj ^nat-long? n] (boolean-array n))
  (^first [^byte-array?    obj ^nat-long? n] (byte-array    n))
  (^first [^char-array?    obj ^nat-long? n] (char-array    n))
  (^first [^object-array?  obj ^nat-long? n] (object-array  n))))

(defnt ->array
  #?(:cljs ([x ct] (TODO)))
  #?(:clj (^boolean-array? [^boolean?        t ^nat-long? ct] (boolean-array ct)))
  #?(:clj (^byte-array?    [^byte?           t ^nat-long? ct] (byte-array    ct)))
  #?(:clj (^char-array?    [^char?           t ^nat-long? ct] (char-array    ct)))
  #?(:clj (^short-array?   [^short?          t ^nat-long? ct] (short-array   ct)))
  #?(:clj (^int-array?     [^int?            t ^nat-long? ct] (int-array     ct)))
  #?(:clj (^long-array?    [^long?           t ^nat-long? ct] (long-array    ct)))
  #?(:clj (^float-array?   [^float?          t ^nat-long? ct] (float-array   ct)))
  #?(:clj (^double-array?  [^double?         t ^nat-long? ct] (double-array  ct)))
  #?(:clj (                [^java.lang.Class c ^nat-long? ct] (make-array c  ct)))) ; object-array is subsumed into this

(defnt subseq
  "Returns a view of @`coll` from @`a` to @`b` in O(1) time."
          ([^+vec?       coll ^nat-long? a             ] (subvec coll a  ))
          ([^+vec?       coll ^nat-long? a ^nat-long? b] (subvec coll a b))
  #?(:clj ([^array-list? coll ^nat-long? a             ] (.subList coll a (lasti coll))))
  #?(:clj ([^array-list? coll ^nat-long? a ^nat-long? b] (.subList coll a b)))
  #?(:clj ([^string?     coll ^nat-long? a             ] (.subSequence coll a (count coll))))
  #?(:clj ([^string?     coll ^nat-long? a ^nat-long? b] (.subSequence coll a b)))
          ([^reducer?    coll ^nat-long? a             ] (->> coll (drop+ a)))
          ([^reducer?    coll ^nat-long? a ^nat-long? b] (->> coll (drop+ a) (take+ b))))

(defnt getr
  "AKA slice.
   Makes a subcopy of @`coll` from @`a` to @`b` in the most efficient way possible.
   Differs from `subseq` in that it does not simply return a view in O(1) time."
  {:todo {0 "Slice for CLJS arrays"}}
  ; Inclusive range
          ([^string?     coll ^nat-long? a             ] (.substring coll a (count coll)))
          ([^string?     coll ^nat-long? a ^nat-long? b] (.substring coll a (inc b)))
          ([^reducer?    coll ^nat-long? a ^nat-long? b] (->> coll (take+ b) (drop+ a)))
          ([^+vec?       coll ^nat-long? a             ] (subsvec coll a (count coll)))
          ([^+vec?       coll ^nat-long? a ^nat-long? b] (subsvec coll a (inc b)))
  ; TODO 0
  #?(:clj (^first [^array?      coll ^nat-long? a ^nat-long? b]
            (let [arr-f (array-of-type coll (core/long (inc (- b a))))] ; TODO make long cast unnecessary
              (System/arraycopy coll a arr-f 0
                (inc (- b a)))
              arr-f)))
          ([^:obj        coll ^nat-long? a             ] (->> coll (drop a)))
          ([^:obj        coll ^nat-long? a ^nat-long? b] (->> coll (take b) (drop a))))

(defnt rest
  "Eager rest."
  ([^keyword? k]    (-> k name core/rest))
  ([^symbol?  s]    (-> s name core/rest))
  ([^reducer? coll] (drop+ 1 coll))
  ([^string?  coll] (getr coll 1 (lasti coll)))
  ([^+vec?    coll] (getr coll 1 (lasti coll)))
  ([^array?   coll] (getr coll 1 (core/long (lasti coll)))) ; TODO use macro |long|
  ([          coll] (core/rest coll)))

#?(:clj (defalias popl rest))

(defnt index-of
  {:todo ["Add 3-arity for |index-of-from|"]}
  ; TODO Reflection warning - call to method indexOf on clojure.lang.IPersistentVector can't be resolved (no such method).
  ([^+vec?   coll elem] (let [i (.indexOf coll elem)] (if (= i -1) nil i)))
  ([^string? coll elem]
    (cond (string? elem)
          (let [i (.indexOf coll ^String elem)] (if (= i -1) nil i))
          (pattern? elem)
          #?(:clj  (let [^java.util.regex.Matcher matcher
                          (re-matcher elem coll)]
                     (when (.find matcher)
                       (.start matcher)))
             :cljs (throw (->ex :unimplemented
                                (str "|index-of| not implemented for " (class coll) " on " (class elem))
                                (kmap coll elem))))
          :else (throw (->ex :unimplemented
                             (str "|last-index-of| not implemented for String on" (class elem))
                             (kmap coll elem))))))

; Spent too much time on this...
; (defn nth-index-of [super sub n]
;   (reducei
;     (fn [[sub-matched i-found indices-found :as state] elem i]
;       (let-alias [sub-match?      (= elem (get super i))
;                   match-complete? (= (inc sub-matched) (count sub))
;                   nth-index?      (= (inc indices-found) n)]
;         (if sub-match?
;             (let [[sub-matched-n+1 i-found-n+1 indices-found-n+1 :as state]
;                    []])
;             (if match-complete?
;                   (if nth-index?
;                       i-found))
;             (if (= n (lasti super))
;                 nil
;                 state))))
;     [0 0 nil]
;     super))

(defnt last-index-of
   ; TODO Reflection warning - call to method lastIndexOf on clojure.lang.IPersistentVector can't be resolved (no such method).
  ([^+vec?   coll elem] (let [i (.lastIndexOf coll elem)] (if (= i -1) nil i)))
  ([^string? coll elem]
    (cond (string? elem)
          (let [i (.lastIndexOf coll ^String elem)] (if (= i -1) nil i))
          :else (throw (->ex :unimplemented
                             (str "|last-index-of| not implemented for String on" (class elem))
                             (kmap coll elem))))))

(defnt containsk?
  {:imported "clojure.lang.RT.contains"}
           ([#{string? array?}                            coll ^nat-long? n] (and (>= n 0) (<  (count coll))))
  #?(:clj  ([#{clojure.lang.Associative    java.util.Map} coll           k] (.containsKey   coll k)))
  #?(:clj  ([#{clojure.lang.IPersistentSet java.util.Set} coll           k] (.contains      coll k)))
  #?(:cljs ([#{+set? +map?}                               coll           k] (core/contains? coll k))) ; TODO find out how to make faster
           ([^:obj                                        coll           k]
             (if (nil? coll)
                 false
                 (throw (->ex :not-supported
                          (str "contains? not supported on type: " (-> coll class)))))))

#?(:clj (defalias contains? containsk?))

(defnt containsv?
  ([^string?  coll elem]
    (and (nnil? elem) (index-of coll elem)))
  ([^pattern? coll elem]
    (nnil? (re-find elem coll)))
  ([          coll elem]
    (seq-or (fn= elem) coll)))

; static Object getFrom(Object coll, Object key, Object notFound){
;   else if(coll instanceof Map) {
;     Map m = (Map) coll;
;     if(m.containsKey(key))
;       return m.get(key);
;     return notFound;
;   }
;   else if(coll instanceof IPersistentSet) {
;     IPersistentSet set = (IPersistentSet) coll;
;     if(set.contains(key))
;       return set.get(key);
;     return notFound;
;   }
;   else if(key instanceof Number && (coll instanceof String || coll.getClass().isArray())) {
;     int n = ((Number) key).intValue();
;     return n >= 0 && n < count(coll) ? nth(coll, n) : notFound;
;   }
;   return notFound;

; }

(defnt aget  ;  (java.lang.reflect.Array/get coll n) is about 4 times faster than core/get
  "Basically this is the whole quantum/java Array file.
   Takes only 1-2 seconds to generate and compile this."
          ([^array-1d? x #?(:clj #{nat-int?}) i1]
            (#?(:clj  Array/get
                :cljs core/aget) x i1))
  #?(:clj ([#{array-2d? array-3d? array-4d? array-5d? array-6d? array-7d? array-8d? array-9d? array-10d?} x
            ^int i1]
            (Array/get x i1))))

#?(:clj ; TODO macro to de-repetitivize
(defnt aget-in*
  ([#{array-1d? array-2d? array-3d? array-4d? array-5d? array-6d? array-7d? array-8d? array-9d? array-10d?} x
    ^int i1]
    (Array/get x i1))
  ([#{array-2d? array-3d? array-4d? array-5d? array-6d? array-7d? array-8d? array-9d? array-10d?} x
    ^int i1 ^int i2]
    (Array/get x i1 i2))
  ([#{array-3d? array-4d? array-5d? array-6d? array-7d? array-8d? array-9d? array-10d?} x
    ^int i1 ^int i2 ^int i3]
    (Array/get x i1 i2 i3))
  ([#{array-4d? array-5d? array-6d? array-7d? array-8d? array-9d? array-10d?} x
    ^int i1 ^int i2 ^int i3 ^int i4]
    (Array/get x i1 i2 i3 i4))
  ([#{array-5d? array-6d? array-7d? array-8d? array-9d? array-10d?} x
    ^int i1 ^int i2 ^int i3 ^int i4 ^int i5]
    (Array/get x i1 i2 i3 i4 i5))
  ([#{array-6d? array-7d? array-8d? array-9d? array-10d?} x
    ^int i1 ^int i2 ^int i3 ^int i4 ^int i5
    ^int i6]
    (Array/get x i1 i2 i3 i4 i5 i6))
  ([#{array-7d? array-8d? array-9d? array-10d?} x
    ^int i1 ^int i2 ^int i3 ^int i4 ^int i5
    ^int i6 ^int i7]
    (Array/get x i1 i2 i3 i4 i5 i6 i7))
  ([#{array-8d? array-9d? array-10d?} x
    ^int i1 ^int i2 ^int i3 ^int i4 ^int i5
    ^int i6 ^int i7 ^int i8]
    (Array/get x i1 i2 i3 i4 i5 i6 i7 i8))
  ([#{array-9d? array-10d?} x
    ^int i1 ^int i2 ^int i3 ^int i4 ^int i5
    ^int i6 ^int i7 ^int i8 ^int i9]
    (Array/get x i1 i2 i3 i4 i5 i6 i7 i8 i9))
  ([#{array-10d?} x
    ^int i1 ^int i2 ^int i3 ^int i4 ^int i5
    ^int i6 ^int i7 ^int i8 ^int i9 ^int i10]
    (Array/get x i1 i2 i3 i4 i5 i6 i7 i8 i9 i10))))

#?(:cljs (defn aget-in*-protocol [arr & ks] (TODO)))

(defnt get
  {:imported "clojure.lang.RT/get"}
  #?(:clj  ([^clojure.lang.ILookup           x            k             ] (.valAt x k)))
  #?(:clj  ([^clojure.lang.ILookup           x            k if-not-found] (.valAt x k if-not-found)))
  #?(:clj  ([#{java.util.Map clojure.lang.IPersistentSet}
                                             x            k             ] (.get x k)))
           ([^string?                        x ^nat-long? i if-not-found] (if (>= i (count x)) if-not-found (.charAt x i)))
  #?(:clj  ([^array-list?                    x ^nat-long? i if-not-found] (if (>= i (count x)) if-not-found (.get    x i))))
           ([#{string? #?(:clj array-list?)} x ^nat-long? i             ] (get      x i nil))

           ([^array?                         x ^nat-long? i             ] (aget     x i             ))
           ([^seq?                           x            i             ] (core/nth x i nil         ))
           ([^seq?                           x            i if-not-found] (core/nth x i if-not-found))
           ; TODO look at clojure.lang.RT/get for how to handle these edge cases efficiently
  #?(:cljs ([^nil?                           x            i             ] (core/get x i nil         )))
  #?(:cljs ([^nil?                           x            i if-not-found] (core/get x i if-not-found)))
           ([                                x            i             ] (core/get x i nil         ))
           ([                                x            i if-not-found] (core/get x i if-not-found)))

(defnt nth
  ; TODO import clojure.lang.RT/nth
  ([#{+vec? string? array-list? #_array? ; for now, because java.lang.VerifyError: reify method: Nth signature: ([Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;) Incompatible object argument for function call
      seq?} coll i] (get coll i))
  ([^reducer? coll ^nat-long? i]
    (let [i' (volatile! 0)]
      (reduce (rfn [ret x] (if (= @i' i)
                                (reduced x)
                                (do (vswap! i' inc)
                                    ret)))
        nil coll)))
  ([coll i] (core/nth coll i))
  #_([#{clojure.data.avl.AVLSet
      clojure.data.avl.AVLMap
      java.util.Map
      clojure.lang.IPersistentSet} coll i] (core/nth coll i)))

#_(defnt aget-in ; TODO construct using a macro
  "Haven't fixed reflection issues for unused code paths. Also not performant."
  ([#{array? array-2d? array-3d? array-4d? array-5d? array-6d? array-7d? array-8d? array-9d? array-10d?} x
    indices]
   (condp = (count indices)
     1  (aget-in* x (int (get indices 0))                )
     2  (aget-in* x (int (get indices 0)) (int (get indices 1)))
     3  (aget-in* x (int (get indices 0)) (int (get indices 1)) (int (get indices 2)))
     4  (aget-in* x (int (get indices 0)) (int (get indices 1)) (int (get indices 2)) (int (get indices 3)))
     5  (aget-in* x (int (get indices 0)) (int (get indices 1)) (int (get indices 2)) (int (get indices 3)) (int (get indices 4)))
     6  (aget-in* x (int (get indices 0)) (int (get indices 1)) (int (get indices 2)) (int (get indices 3)) (int (get indices 4)) (int (get indices 5)))
     7  (aget-in* x (int (get indices 0)) (int (get indices 1)) (int (get indices 2)) (int (get indices 3)) (int (get indices 4)) (int (get indices 5)) (int (get indices 6)))
     8  (aget-in* x (int (get indices 0)) (int (get indices 1)) (int (get indices 2)) (int (get indices 3)) (int (get indices 4)) (int (get indices 5)) (int (get indices 6)) (int (get indices 7)))
     9  (aget-in* x (int (get indices 0)) (int (get indices 1)) (int (get indices 2)) (int (get indices 3)) (int (get indices 4)) (int (get indices 5)) (int (get indices 6)) (int (get indices 7)) (int (get indices 8)))
     10 (aget-in* x (int (get indices 0)) (int (get indices 1)) (int (get indices 2)) (int (get indices 3)) (int (get indices 4)) (int (get indices 5)) (int (get indices 6)) (int (get indices 7)) (int (get indices 8)) (int (get indices 9)))
     0 (throw (->ex "Indices can't be empty"))
     :else (throw (->ex "Indices count can't be >10")))))

#?(:clj  (defnt swap!
           ([^IAtom x f      ] (.swap x f      ))
           ([^IAtom x f a0   ] (.swap x f a0   ))
           ([^IAtom x f a0 a1] (.swap x f a0 a1)))
   :cljs (defalias swap! core/swap!))

(defalias doto! swap!)

#?(:clj  (defnt reset!
           ([#{IAtom clojure.lang.Volatile} x          v] (.reset x v) v)
           ([^AtomicReference               x          v] (.set   x v) v)
           ([^AtomicBoolean                 x ^boolean v] (.set   x v) v)
           ([^AtomicInteger                 x ^int     v] (.set   x v) v)
           ([^AtomicLong                    x ^long    v] (.set   x v) v))
   :cljs (defalias reset! core/reset!))

(defnt aset!
  "Yay, |aset| no longer causes reflection or needs type hints!"
  {:performance "|java.lang.reflect.Array/set| is 26 times faster
                  than 'normal' reflection"
   :todo #{"have the semantics such that (aset! arr v i0 i1 i2) is possible"}}
  #?(:cljs (^first [^array-1d?      coll            i          v] (aset coll i v             ) coll))
  #?(:clj  (^first [^boolean-array? coll ^nat-long? i ^boolean v] (aset coll i v             ) coll))
  #?(:clj  (^first [^byte-array?    coll ^nat-long? i ^byte    v] (aset coll i (core/byte  v)) coll)) ; TODO make this not required
  #?(:clj  (^first [^char-array?    coll ^nat-long? i ^char    v] (aset coll i v)              coll))
  #?(:clj  (^first [^short-array?   coll ^nat-long? i ^short   v] (aset coll i (core/short v)) coll)) ; TODO make this not required
  #?(:clj  (^first [^int-array?     coll ^nat-long? i ^int     v] (aset coll i v             ) coll))
  #?(:clj  (^first [^long-array?    coll ^nat-long? i ^long    v] (aset coll i v             ) coll))
  #?(:clj  (^first [^float-array?   coll ^nat-long? i ^float   v] (aset coll i v             ) coll))
  #?(:clj  (^first [^double-array?  coll ^nat-long? i ^double  v] (aset coll i v             ) coll))
  #?(:clj  (^first [^object-array?  coll ^nat-long? i          v] (aset coll i v             ) coll))
  #?(:clj  (^first [                coll ^nat-long? i          v] (java.lang.reflect.Array/set coll i v) coll)))

; TODO
; (defnt aset-in!)

; TODO assoc-in and assoc-in! for files
(defnt assoc!
  {:todo ["Remove reflection for |aset!|."]}
  #?(:cljs (^first [^array-1d?      coll            k          v] (aset!       coll k v)))
  #?(:clj  (^first [^boolean-array? coll ^nat-long? k ^boolean v] (aset!       coll k v)))
  #?(:clj  (^first [^byte-array?    coll ^nat-long? k ^byte    v] (aset!       coll k v)))
  #?(:clj  (^first [^char-array?    coll ^nat-long? k ^char    v] (aset!       coll k v)))
  #?(:clj  (^first [^short-array?   coll ^nat-long? k ^short   v] (aset!       coll k v)))
  #?(:clj  (^first [^int-array?     coll ^nat-long? k ^int     v] (aset!       coll k v)))
  #?(:clj  (^first [^long-array?    coll ^nat-long? k ^long    v] (aset!       coll k v)))
  #?(:clj  (^first [^float-array?   coll ^nat-long? k ^float   v] (aset!       coll k v)))
  #?(:clj  (^first [^double-array?  coll ^nat-long? k ^double  v] (aset!       coll k v)))
  #?(:clj  (^first [^object-array?  coll ^nat-long? k          v] (aset!       coll k v)))
           (^first [^transient?     coll            k          v] (core/assoc! coll k v))
           (       [^atom?          coll            k          v] (swap! coll core/assoc k v)))

(defnt assoc!*
  #?(:cljs (^first [^array-1d?      coll            k          v] (assoc!     coll k v)))
  #?(:clj  (^first [^boolean-array? coll ^nat-long? k ^boolean v] (assoc!     coll k v)))
  #?(:clj  (^first [^byte-array?    coll ^nat-long? k ^byte    v] (assoc!     coll k v)))
  #?(:clj  (^first [^char-array?    coll ^nat-long? k ^char    v] (assoc!     coll k v)))
  #?(:clj  (^first [^short-array?   coll ^nat-long? k ^short   v] (assoc!     coll k v)))
  #?(:clj  (^first [^int-array?     coll ^nat-long? k ^int     v] (assoc!     coll k v)))
  #?(:clj  (^first [^long-array?    coll ^nat-long? k ^long    v] (assoc!     coll k v)))
  #?(:clj  (^first [^float-array?   coll ^nat-long? k ^float   v] (assoc!     coll k v)))
  #?(:clj  (^first [^double-array?  coll ^nat-long? k ^double  v] (assoc!     coll k v)))
  #?(:clj  (^first [^object-array?  coll ^nat-long? k          v] (assoc!     coll k v)))
           (^first [^transient?     coll            k          v] (assoc!     coll k v))
           (       [^atom?          coll            k          v] (assoc!     coll k v))
           (       [                coll            k          v] (core/assoc coll k v)))

(defnt assoc
  {:imported "clojure.lang.RT/assoc"}
  #?(:clj  ([^clojure.lang.Associative x k v] (.assoc x k v)))
  #?(:cljs ([#{+vec? +map?}            x k v] (cljs.core/-assoc x k v)))
  #?(:cljs ([^nil?                     x k v] {k v}))
  #?(:clj  ([                          x k v]
             (if (nil? x)
                 {k v}
                 (throw (->ex :not-supported "`assoc` not supported on this object" {:type (type x)}))))))

(defnt dissoc
  {:imported "clojure.lang.RT/dissoc"}
           ([^+map?                       coll k] (#?(:clj .without :cljs -dissoc ) coll k))
           ([^+set?                       coll x] (#?(:clj .disjoin :cljs -disjoin) coll x))
           ([^+vec?                       coll i]
             (catvec (subvec coll 0 i) (subvec coll (inc (#?(:clj identity* :cljs long) i)) (count coll))))
  #?(:cljs ([^nil?                        coll x] nil))
  #?(:clj  ([                             coll x]
             (if (nil? coll)
                 nil
                 (throw (->ex :not-supported "`dissoc` not supported on this object" {:type (type coll)}))))))

(defnt dissoc!
  ([^transient? coll k  ] (core/dissoc! coll k))
  ([^atom?      coll k  ] (swap! coll (fn&2 dissoc) k)))

(defnt conj!
  ([^transient? coll obj] (core/conj! coll obj))
  ([^atom?      coll obj] (swap! coll core/conj obj)))

(defnt disj!
  ([^transient? coll obj] (core/disj! coll obj))
  ([^atom?      coll obj] (swap! coll disj obj)))

#?(:clj
(defmacro update! [coll i f]
  `(assoc! ~coll ~i (~f (get ~coll ~i)))))

(defnt first
  {:todo #{"Return first element type"}}
  ([^array?                         x] (get x 0))
  ([#{string? #?(:clj array-list?)} x] (get x 0 nil))
  ([#{symbol? keyword?}             x] (if (namespace x) (-> x namespace first) (-> x name first)))
  ([^+vec?                          x] (get x #?(:clj (Long. 0) :cljs 0))) ; to cast it...
  ([^reducer?                       x] (reduce (rfn [_ x'] (reduced x')) nil x))
  ([:else                           x] (core/first x)))

(defalias firstl first) ; TODO not always true

(defnt second
  ([#{string? #?(:clj array-list?)} x] (get x 1 nil))
  ([#{symbol? keyword?}             x] (if (namespace x) (-> x namespace second) (-> x name second)))
  ; 2.8  nanos to (.cast Long _)
  ; 1.26 nanos to (Long. _)
  ([^+vec?                          x] (get x #?(:clj (Long. 1) :cljs 1))) ; to cast it...
  ([^reducer?                       x] (nth x 1))
  ([:else                           x] (core/second x)))

(defnt butlast
  {:todo ["Add support for arrays"
          "Add support for CLJS IPersistentStack"]}
          ([^string?                       x] (getr x 0 (-> x lasti dec)))
  #?(:clj ([^reducer?                      x] (dropr+ 1 x)))
          ; TODO reference to field pop on clojure.lang.APersistentVector$RSeq can't be resolved.
          ([^+vec?                         x] (if (empty? x) (#?(:clj .pop :cljs -pop) x) x))
          ([:else                          x] (core/butlast x)))

(defalias pop  butlast) ; TODO not always correct
(defalias popr butlast)

(defnt last
          ([^string?          coll] (get coll (lasti coll)))
          ([#{symbol? keyword?} x] (-> x name last))
  #?(:clj ([^reducer?         coll] (taker+ 1 coll)))
          ; TODO reference to field peek on clojure.lang.APersistentVector$RSeq can't be resolved.
          ([^+vec?            coll] (#?(:clj .peek :cljs .-peek) coll))
  #?(:clj ([#{#?@(:clj  [array-list? clojure.lang.PersistentVector$TransientVector]
                  :cljs [cljs.core/TransientVector])} coll]
            (get coll (lasti coll))))
          ([:else             coll] (core/last coll)))

(defalias peek   last) ; TODO not always correct
(defalias firstr last)

#?(:clj  (defn array
           {:todo ["Consider efficiency here"]}
           [& args]
           (let [c (-> args first class)]
             (into-array (get tdef/boxed->unboxed-types-evaled c c) args)))
   :cljs (defalias array core/array))


(defn gets [coll indices]
  (->> indices (red/map+ #(get coll %)) (red/join [])))

(def third   (fn1 get 2))
(def fourth  (fn1 get 3))
(def fifth   (fn1 get 4))
(def sixth   (fn1 get 5))
(def seventh (fn1 get 6))
(def eighth  (fn1 get 7))
(def ninth   (fn1 get 8))
(def tenth   (fn1 get 9))

;--------------------------------------------------{           CONJL          }-----------------------------------------------------
; This will take AGES to compile if you try to allow primitives
(defnt conjl
  ([^seq?  coll a          ] (->> coll (cons a)                                             ))
  ([^seq?  coll a b        ] (->> coll (cons b) (cons a)                                    ))
  ([^seq?  coll a b c      ] (->> coll (cons c) (cons b) (cons a)                           ))
  ([^seq?  coll a b c d    ] (->> coll (cons d) (cons c) (cons b) (cons a)                  ))
  ([^seq?  coll a b c d e  ] (->> coll (cons e) (cons d) (cons c) (cons b) (cons a)         ))
  ([^seq?  coll a b c d e f] (->> coll (cons f) (cons e) (cons d) (cons c) (cons b) (cons a)))
  ([^+vec? coll a          ] (catvec (svector a          ) coll))
  ([^+vec? coll a b        ] (catvec (svector a b        ) coll))
  ([^+vec? coll a b c      ] (catvec (svector a b c      ) coll))
  ([^+vec? coll a b c d    ] (catvec (svector a b c d    ) coll))
  ([^+vec? coll a b c d e  ] (catvec (svector a b c d e  ) coll))
  ([^+vec? coll a b c d e f] (catvec (svector a b c d e f) coll))
  ([^+vec? coll a b c d e f & more]
    (reduce (fn [ret elem] (conjl ret elem)) ; should just be |conjl|
      (svector a b c d e f) more)))

; TODO to finish from RT
; (defnt conj
;   ([^IPersistentCollection coll ^Object x]
;     (if (nil? coll)
;         (PersistentList. x)
;         (.cons coll x))))

(defalias conj core/conj)

(defnt conjr
  ([^+vec? coll a    ] (core/conj a    ))
  ([^+vec? coll a b  ] (core/conj a b  ))
  ([^+vec? coll a b c] (core/conj a b c))
  ;([coll a & args] (apply conj a args))
  ([^seq?  coll a    ] (concat coll (list a    )))
  ([^seq?  coll a b  ] (concat coll (list a b  )))
  ([^seq?  coll a b c] (concat coll (list a b c)))
  ;([coll a & args] (concat coll (cons arg args)))
  )

#?(:clj (defnt ^clojure.lang.PersistentVector ->vec
          "513.214568 msecs (vec a1)
           182.745605 msecs (seqspert.vector/array-to-vector a1)"
          ([^array-1d? x] (if (> (count x) parallelism-threshold)
                              (seqspert.vector/array-to-vector x)
                              (vec x))))
   :cljs (defalias ->vec core/vec))

#?(:clj
(defnt ^"[Ljava.lang.Object;" ->arr
  ([^+vec? x] (if (> (count x) parallelism-threshold)
                  (seqspert.vector/vector-to-array x)
                  (into-array Object x)))))

; VECTORS
; 166.884981 msecs (mapv identity v1)
; 106.545886 msecs (seqspert.vector/vmap   identity v1)))
; 22.778568  msecs (seqspert.vector/fjvmap identity v1)

(defn- handle-kv
  [kv f]
  (if (-> kv count (= 2))
      (f)
      (throw (->ex :not-supported "`key/val` not supported on collections of count != 2"
                   {:coll kv :ct (count kv)}))))

(defnt ^:private key*
  {:todo #{"Implement core/key"}}
  #?@(:clj  [([^map-entry?     kv] (core/key kv))
             ([^List           kv] (handle-kv kv #(first kv)))]
      :cljs [([#{+vec? array?} kv] (handle-kv kv #(first kv)))]))

(defnt ^:private val*
  {:todo #{"Implement core/val"}}
  #?@(:clj  [([^map-entry?     kv] (core/val kv))
             ([^List           kv] (handle-kv kv #(second kv)))]
      :cljs [([#{+vec? array?} kv] (handle-kv kv #(second kv)))]))

(defn key ([kv] (when kv (key* kv))) ([k v] k))
(defn val ([kv] (when kv (val* kv))) ([k v] v))

; what about arrays? some transient loop or something
(def reverse (ifn1 reversible? rseq core/reverse))
