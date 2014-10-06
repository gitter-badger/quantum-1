(ns quanta.library.collections
  (:require
    [quanta.library.ns               :as ns    :refer [defalias alias-ns]]
    [quanta.library.numeric          :as num   :refer [greatest least]]
    [quanta.library.type             :as type  :refer :all]
    [quanta.library.data.vector      :as vec   :refer [vector+? subvec+ catvec conjl]]
    [quanta.library.data.map         :as map   :refer [ordered-map map-entry]]
    [quanta.library.data.set         :as set   :refer [ordered-set]]
    [quanta.library.collections.core :as core]
    [quanta.library.logic            :as log   :refer :all]
    [quanta.library.string           :as str]
    [quanta.library.function         :as fn    :refer :all]
    [quanta.library.error            :as err   :refer [try+ throw+]]
    [clojure.pprint                            :refer [pprint]]
    [clojure.walk                    :as walk]
    [clojure.core.async              :as async :refer [>!! <!! close! chan thread]])
  (:gen-class))
(set! *warn-on-reflection* true)

; drop-from-back-while
;  reverse+ (drop-while+ (eq? [""])) fold+ reverse+ vec+

(alias-ns 'quanta.library.collections.core)
(alias-ns 'quanta.library.reducers)
(defalias merge+ map/merge+)

(defmacro kmap [& ks]
 `(zipmap (map keyword (quote ~ks)) (list ~@ks)))

(defn reduce-2
  "Like |reduce|, but reduces over two items in a collection at a time.

   Its function @func must take three arguments:
   1) The accumulated return value of the reduction function
   2) The                next item in the collection being reduced over
   3) The item after the next item in the collection being reduced over"
  ^{:attribution "Alex Gunnarson"}
  [func init coll] ; not actually implementing of CollReduce... so not as fast...
  (loop [ret init coll-n coll]
    (if (empty? coll-n)
        ret
        (recur (func ret (first coll-n) (second coll-n))
               (-> coll-n rest rest)))))

(defn select
  "Applies a list of functions, @fns, separately to an object, @coll.
   A good use case is returning values from an associative structure with keys as @fns.
   Returns a vector of the results."
  ^{:attribution "Alex Gunnarson"
    :usage "(select {:a 1 :b [3]} :a (compr :b 0)) => [1 3]"}
  [coll & fns]
  ((apply juxt fns) coll))

; TODO: http://clojure.org/cheatsheet (go through)
; (require '[taoensso.encore :as lib+ :refer
;   [swap-in! reset-in!
;    dissoc-in]])
; deftype, defrecord, defprotocol, defstruct, (defmulti, defmethod)-slowish
; ARRAYS - look at hiphip, Array! 
; aget, aset, amap, areduce, aset-(char|boolean|...)
; ETC
; not-every?
; not-any?
; http://clojuredocs.org/clojure_core/clojure.data/diff
; flatten
; reductions
; Zippers (clojure.zip/) - http://clojure.org/cheatsheet
; /zipmap/
; (zipmap [:a :b :c] [1 2 3 4]) => {:a 1 :b 2 :c 3}
; nodes in a tree - treeseq
; split-at, split-with
; clojure.inspector/inspect - graphical Swing inspector on supplied object
; /max-key/ + /min-key/
; (max-key count "asd" "bsd" "dsd" "long word") => "long word"

; TODO: reduce -> reduce+, map -> map+, etc.
; look at pmin|pmax||pfilter-nils|pfilter-dupes|peek|pdistinct
; PARALLEL OPERATIONS
; pmap - not as good as the reducer version (?)
; /pcalls/ - uses /future/
; (pcalls #(fn1 a b) #(fn2 c e) ...) -> (result1 result2 ...)
; /pvalues/ - a convienience macro around pcalls
; (pvalues (fn1 a b)  (fn2 c e) ...) -> (result1 result2 ...)
(defn coll-if [obj]
  (whenf obj (fn-not coll?) vector))
;___________________________________________________________________________________________________________________________________
;=================================================={         LAZY SEQS        }=====================================================
;=================================================={                          }=====================================================
(defalias lseq lazy-seq)
(defn seq-if [obj]
  (condf obj
    (fn-or seq? nil?) identity
    coll?             seq
    :else             list))
(defn lseq+ [obj]
  (condf obj
    (fn-or seq? nil? coll?) #(lseq %) ; because can't take value of a macro
    :else (fn-> list lseq first)))
(defn unchunk
  "Takes a seqable and returns a lazy sequence that
   is maximally lazy and doesn't realize elements due to either
   chunking or apply.

   Useful when you don't want chunking, for instance,
   (first awesome-website? (map slurp <a-bunch-of-urls>))
   may slurp up to 31 unneed webpages, whereas
   (first awesome-website? (map slurp (unchunk <a-bunch-of-urls>)))
   is guaranteed to stop slurping after the first awesome website.

  Taken from http://stackoverflow.com/questions/3407876/how-do-i-avoid-clojures-chunking-behavior-for-lazy-seqs-that-i-want-to-short-ci"
  ^{:attribution "prismatic.plumbing"}
  [s]
  (when (seq s)
    (cons (first s)
          (lseq (s rest unchunk)))))
;___________________________________________________________________________________________________________________________________
;=================================================={  POSITION IN COLLECTION  }=====================================================
;=================================================={ first, rest, nth, get ...}=====================================================
(defmacro get!
  "Get the value in java.util.Map m under key k.  If the key is not present,
  set the value to the result of default-expr and return it.  Useful for
  constructing mutable nested structures on the fly.
  USAGE:
  (.add ^List (get! m :k (java.util.ArrayList.)) :foo)"
 ^{:attribution "prismatic.plumbing"}
  [m k default-expr]
  `(let [^java.util.Map m# ~m k# ~k]
     (or (.get m# k#)
         (let [nv# ~default-expr]
           (.put m# k# nv#)
           nv#))))
; /defrecord/ can produce dramatically faster code.
; Calling a protocol method like fixo-peek on a record type
; that implements it inline can be several times faster
; than calling the same method on an object that implements it via an extend form.
; TODO: Define an interface which is both a delay and a reducer

; (def h ; ONLY FOR MULTIMETHODS... sorry...
;   (-> (make-hierarchy)
;       (derive ::Reducer clojure.core.protocols.CollReduce)
;       (derive ::Reducer quanta.library.reducers.Folder)
;       (derive ::FoldPreable ::Reducer)
;       (derive ::FoldPreable Delay)))
(defn- nth-red
  "|nth| implemented in terms of |reduce+|."
  ^{:deprecated  true
    :performance "Twice as slow as |nth|"}
  [coll n]
  (let [nn (atom 0)]
    (->> coll
         (reduce+
           (fn
             ([ret elem]
              (if (= n @nn)
                  (reduced elem)
                  (do (swap! nn inc) ret)))
             ([ret k v]
               (if (= n @nn)
                   (reduced [k v])
                   (do (swap! nn inc) ret))))
           []))))
(defn key+
  "Like |key| but more robust."
  ^{:attribution "Alex Gunnarson"}
  ([obj] 
    (try+
      (ifn obj vector? first+ key)
      (catch Object _ (println "Error in key+ with obj:" obj) nil)))
  ([k v] k)) ; For use with kv-reduce
(defn val+
  "Like |val| but more robust."
  ^{:attribution "Alex Gunnarson"}
  ([obj]
    (try+
      (ifn obj vector? second+ key)
      (catch Object _ (println "Error in val+ with obj:" obj) nil)))
  ([k v] v)) ; For use with kv-reduce

(defn fkey+ [m]
  (-> m first key+))
(defn fval+ [m]
  (-> m first val+))

; should use take+ and drop+ with strings...
; (#(str/subs+ % 0 (str/index-of " " %))) ; take-until


; for /subseq/, the coll must be a sorted collection (e.g., not a [], but rather a sorted-map or sorted-set)
; test(s) one of <, <=, > or >=

; /nthrest/
; (nthrest (range 10) 5) => (5 6 7 8 9)

; TODO: get-in from clojure, make it better
(defn get-in+ [coll [iden :as keys-0]] ; implement recursively
  (if (= iden identity)
      coll
      (get-in coll keys-0)))
(defn reverse+ [coll] ; what about arrays? some transient loop or something
  (ifn coll reversible? rseq reverse))
(def single?
  "Does coll have only one element?"
  (fn-and seq (fn-not next)))
;___________________________________________________________________________________________________________________________________
;=================================================={   ADDITIVE OPERATIONS    }=====================================================
;=================================================={    conj, cons, assoc     }=====================================================
; this doesn't quite work yet
; (defn conjl [coll elem]
;   (cond (vector+? coll)
;         (concat+ elem coll)
;         (vector? coll) ; does this check if it's an RRB vec? yes
;         (concat+ elem (vec+ coll))
;         :else (conj coll elem)))
; conjl [o val] - like clojure.core/conj, but always from left
; conjr [o val] - like clojure.core/conj, but always from right
;___________________________________________________________________________________________________________________________________
;=================================================={           MAP            }=====================================================
;=================================================={                          }=====================================================

;___________________________________________________________________________________________________________________________________
;=================================================={           MERGE          }=====================================================
;=================================================={      zipmap, zipvec      }=====================================================
; (defn zipvec+ [& colls-0] ; (map vector [] [] [] []) ; 1.487238 ms for zipvec+ vs. 1.628670 ms for doall + map-vector. Yay! :D
;    (let [colls (->> colls-0 (map+ fold+) fold+)]
;      (for+ [n (range 0 (count+ (get colls 0)))] ; should be easy, because count+ will be O(1) with folded colls
;        (->> colls
;             (map (f*n get+ n)))))) ; get+ doesn't take long at all - it was a fluke; also, apparently can't use map+ within for+... weird...
;                                    ; 234.462665 ms if you realize them
; (defn zipfor- [& colls-0] ;  [[1 2 3] [4 5 6] [7 8 9]]
;   (let [colls (->> colls-0 (map+ fold+) fold+) ; nested /for/s, no
;         rng   (range 0 (-> colls count+ dec))]
;     (for+   [n  rng] ; [[1 2 3] [4 5 6] [7 8 9]]
;       (for+ [cn rng] ; ((1 4 7) (4 5 6) ...)
;         (-> colls (get cn) (get n))))))
; ; (zipvec-- [[1 2 3] [4 5 6] [7 8 9]])
; (defn zipvec-- [& colls-0] ; nested /map/s, no
;   (let [colls (vec+ colls-0)]
;     (map+
;       (fn [n]
;         (map+ (getf+ n) colls))
;       (range 0 (inc 2)))))
; /merge/ just doesn't scale up well and you end up doing a significant amount of merging.
; Currently a merge operation takes elements one at a time from one map, and adds them one at a time to another.
; This is incredibly wasteful for large maps as you already have two
; "pre-sorted" tries, and can pair each branch together recursively. If
; each branch node stored the number of child nodes, then you can assign
; different threads to work on different branches as well. This would be
; perfect for reducers, but from a quick look it didn't appear that any
; of the key internals were exposed to be taken advantage of.

; http://grokbase.com/t/gg/clojure/12axvre86w/reduce-reduce-kv-map-mapv-reducers-map-and-nil
; /fold-into-map/ and fold-into-map-with would be wonderful and I tried to
; implement the former along the lines of fold-into-vec, but the performance was
; abysmal. I am now using fold-into-vec + r/map with zipmap which is better, but
; I wouldn't consider that optimal.

; /merge-with/ ; merge by calling function f on the vals
; (merge-with +
;   {:a 1  :b 2}
;   {:a 9  :b 98 :c 0})
; => {:c 0, :a 10, :b 100}
; https://groups.google.com/forum/#!searchin/clojure/reducer|sort:date/clojure/kODtfFDf4hs/CqbO3waRI3cJ
; This means that we can now reduce large datasets into sets/maps more quickly in parallel than we can in serial :)
; As an added benefit, because splice reuses as much of the internal structure of both inputs as possible,
; its impact in terms of heap consumption and churn is less - although I think that a full implementation might
; add some Java-side code complexity.
; 'into' does use transient - but as you can see from the code above, simply rebulds all the associations
; from one side into the other, whereas 'splice' interpolates the underlying tries building new trie nodes
; where appropriate. This makes it faster and more memory efficient as evidenced in the (into c d) vs (splice c d) timings above.
; MIKERA:
; Same technique can probably be used to accelerate "merge" significantly  which is a pretty common operation when you
; are building map-like structures.
; You are right about merge:
; user=> (def m1 (apply hash-map (range 10000000)))
; #'user/m1
; user=> (def m2 (apply hash-map (range 5000000 15000000)))
; #'user/m2
; user=> (time (def m3 (merge m1 m2)))
; "Elapsed time: 5432.184582 msecs"
; #'user/m3
; user=> (time (def m4 (clojure.lang.PersistentHashMap/splice m1 m2)))
; "Elapsed time: 1064.268269 msecs"
; I think we should try and get this into Clojure ASAP.
; I've started doing some more serious testing and have not encountered any problems so far.
; I was a bit worried about the interaction of splice and transient/persistent!, but have not encountered any problems yet. 
; So, having broken the back of fast re-combination of hash sets and maps, I wanted to take a look at doing a similar sort
; of thing for vectors - another type of seq that I use very heavily in this sort of situation.
; , I would also be keen to investigate my idea about the efficient 'cleave'-ing of tree-based seqs so that they can be
; used as inputs to the reducers library, as mentioned in my original post.
; https://github.com/JulesGosnell/clojure/blob/master/src/jvm/clojure/lang/PersistentHashMap.java
; https://github.com/JulesGosnell/clojure/blob/master/src/jvm/clojure/lang/PersistentHashSet.java

; (def a (into+ [] (range 1000000)))
 
; (do (bench (reduce conj #{} a)) nil) ;; 9225 ms
; (do (bench (persistent! (reduce conj! (transient #{}) a))) nil) ;; 5981 ms
; (do (bench (into #{} a)) nil) ;; 6056 ms
 
; ; (def n (/ (count a) (.availableProcessors (Runtime/getRuntime)))) ;; = 625000
 
; (do (bench (r/fold   (monoid into hash-set) conj a)) nil) ;; 9639 ms
; (do (bench (r/fold n (monoid into hash-set) conj a)) nil) ;; 6859 ms
; (do (bench (r/fold   (monoid (fn [r l](clojure.lang.PersistentHashSet/splice r l)) hash-set) conj a)) nil) ;; 3654 ms
; (do (bench (r/fold n (monoid (fn [r l](clojure.lang.PersistentHashSet/splice r l)) hash-set) conj a)) nil) ;; 3288 ms

(defn merge-with+
  "Like merge-with, but the merging function takes the key being merged
   as the first argument"
   ^{:attribution  "prismatic.plumbing"
     :contributors ["Alex Gunnarson"]}
  [f & maps]
  (when (some identity maps)
    (let [merge-entry
           (fn [m e]
             (let [k (key e) v (val e)]
               (if (contains? m k)
                 (assoc m k (f k (get m k) v))
                 (assoc m k v))))
          merge2
            (fn ([] {})
                ([m1 m2]
                 (reduce+ merge-entry (or m1 {}) (seq m2))))]
      (reduce+ merge2 maps))))
;___________________________________________________________________________________________________________________________________
;=================================================={      CONCATENATION       }=====================================================
;=================================================={ cat, fold, (map|con)cat  }=====================================================
; !!!!!!!!!!-----NEEDS OPTIMIZATION-----!!!!!!!!!!
(defn- concat++
  ([coll]
    (try (reduce+ catvec coll)
      (catch Exception e (reduce+ (zeroid into+ []) coll))))
  ([coll & colls]
    (try (apply catvec coll colls)
      (catch Exception e (into+ [] coll colls)))))
;  Use original vectors until they are split. Subvec-orig below a certain range? Before the inflection point of log-n
;___________________________________________________________________________________________________________________________________
;=================================================={         FLATTEN          }=====================================================
;=================================================={                          }=====================================================
; !!!!!!!!!!-----NEEDS OPTIMIZATION-----!!!!!!!!!!
(defn- flatten-map
  "Transform a nested map into a seq of [keyseq leaf-val] pairs"
  ^{:attribution "prismatic.plumbing"}
  [m]
  (when m
    ((fn flatten-helper [keyseq m]
       (when m
         (if (map? m)
             (mapcat (fn [[k v]] (flatten-helper (conj keyseq k) v)) m) ; terrible
             [[keyseq m]])))
     [] m)))
; !!!!!!!!!!-----NEEDS OPTIMIZATION-----!!!!!!!!!!
(defn- unflatten
  "Transform a seq of [keyseq leaf-val] pairs into a nested map.
   If one keyseq is a prefix of another, you're on your own."
   ^{:attribution "prismatic.plumbing"}
  [s]
  (reduce+
    (fn [m [ks v]]
      (if (seq ks)
          (assoc-in m ks v)
          v))
    {} s))
;___________________________________________________________________________________________________________________________________
;=================================================={  FINDING IN COLLECTION   }=====================================================
;=================================================={  in?, index-of, find ... }=====================================================
(defn in? [elem coll]
  (if (string? coll)
      (.contains ^String coll ^String elem)
      (some (eq? elem) coll)))
; index-of-from [o val index-from] - index-of, starting at index-from
(defn contains-or? [coll elems]
  (apply-or (map (partial contains? coll) elems)))
(defn get-keys [m obj]
  (reduce+
    (fn [ret k v] (if (identical? obj v) (conj ret k) ret))
    [] m))
(defn get-key [m obj] (-> m (get-keys obj) first))
; /find/ Returns the map entry for key, or nil if key not present
; (find {:b 2 :a 1 :c 3} :a) => [:a 1]
; (select-keys {:a 1 :b 2} [:a :c]) =>  {:a 1}
; /frequencies/
; (frequencies ['a 'b 'a 'a])
; {a 3, b 1}
; /partition-by/
; splits the coll each time f returns a new value
; (partition-by odd? [1 1 1 2 2 3 3])
; => ((1 1 1) (2 2) (3 3)) /lseq/
;___________________________________________________________________________________________________________________________________
;=================================================={            MAP           }=====================================================
;=================================================={                          }=====================================================
(defn map-kv
  "Maps a function over the keys or values of an associative collection.
   Modified from the original."
  ^{:attribution "weavejester.medley"
    :contributors ["Alex Gunnarson"]}
  [f coll kv-fn]
  (->> coll empty transient (#(reduce+ kv-fn % coll))) persistent!)
(defn map-keys [f coll] (map-kv f coll #(assoc! %1 (f %2) %3)))
(defn map-vals [f coll] (map-kv f coll #(assoc! %1 %2 (f %3))))
;___________________________________________________________________________________________________________________________________
;=================================================={  FILTER + REMOVE + KEEP  }=====================================================
;=================================================={                          }=====================================================
(defn- filter-kv*
  "Returns a new associative collection of the items in coll for which
  `(pred (key item))` (or `(pred (val item))`) returns true.
  Modified from the original." ; make it a reducer
  ^{:attribution "weavejester.medley"}
  [pred coll filter-kw]
  (let [filter-fn
         (filter-kw
           {:keys #(if (pred %2) (assoc! %1 %2 %3) %1)
            :vals #(if (pred %3) (assoc! %1 %2 %3) %1)})]
    (persistent!
      (reduce+ filter-fn
        (transient (empty coll))
        coll)))) 
(defn filter-keys+ [pred coll] (filter-kv* pred              coll :keys))
(defn remove-keys+ [pred coll] (filter-kv* (complement pred) coll :keys))
(defn filter-vals+ [pred coll] (filter-kv* pred              coll :vals))
(defn remove-keys+ [pred coll] (filter-kv* (complement pred) coll :vals))
; !!!!!!!!!!-----NEEDS OPTIMIZATION-----!!!!!!!!!!
(defn- most?
  "Like 'every?' and 'some' but for 'most' items in coll.
   Most defaults to 0.8 for 80% true."
  ^{:attribution "thebusby.bagotricks"}
  ([f coll] (most? f 0.8 coll))
  ([f percent coll]
     (let [all  (count+ coll)
           good (count+ (filter+ f coll))] ; count+ is implemented in terms of reduce+ - don't worry
       (if (zero? all)
         false
         (> (/ good all) percent)))))
;___________________________________________________________________________________________________________________________________
;=================================================={     PARTITION, GROUP     }=====================================================
;=================================================={       incl. slice        }=====================================================
; slice-from [o start] - like slice, but until the end of o
; slice-to [o end] - like slice, but from the beginning of o
; !!!!!!!!!!-----NEEDS OPTIMIZATION-----!!!!!!!!!!
(defn slice ; TODO: use transients for this
  "Divide coll into n approximately equal slices.
   Like partition."
  ^{:attribution "flatland.useful.seq"}
  [n-0 coll]
  (loop [n-n n-0 slices [] items (vec+ coll)]
    (if (empty? items)
      slices
      (let [size (Math/ceil (/ (count+ items) n-n))]
        (recur (dec n-n)
               (conj slices (subvec+ items 0 size))
               (subvec+ items size))))))
; /partition/
; (partition 4 (range 20))
; => ((0 1 2 3) (4 5 6 7) (8 9 10 11) (12 13 14 15) (16 17 18 19))
; (partition 4 6 ["a" "b" "c" "d"] (range 20))
; => ((0 1 2 3) (6 7 8 9) (12 13 14 15) (18 19 "a" "b"))
; /partition-all/
; Returns a lazy sequence of lists like partition, but may include
; partitions with fewer than n items at the end.
; (partition-all 4 [0 1 2 3 4 5 6 7 8 9])
; => ((0 1 2 3) (4 5 6 7) (8 9))
;___________________________________________________________________________________________________________________________________
;=================================================={  DIFFERENTIAL OPERATIONS }=====================================================
;=================================================={     take, drop, split    }=====================================================
; /take-nth/
; (take-nth 2 (range 10))
; => (0 2 4 6 8)
; /cycle/
; (take 5 (cycle ["a" "b"]))
; => ("a" "b" "a" "b" "a")
; /take-last/ ; turn this into a subvec
; (take-last 2 [1 2 3 4]) => (3 4)
; /last/    is a limiting case (1) of take-last
; /drop-last/ ; (drop-last 2 [1 2 3 4]) => (1 2)
; /butlast/ is a limiting case (1) of drop-last

; splice [o index n val] - fast remove and insert in one go
; splice-arr [o index n val-arr] - fast remove and insert in one go
; insert-before [o index val] - insert one item inside coll
; insert-before-arr [o index val] - insert array of items inside coll
; remove-at [o index] - remove one item from index pos
; remove-n [o index n] - remove n items starting at index pos
; triml [o n] - trims n items from left
; trimr [o n] - trims n items from right
; trim [o nl nr] - trims nl items from left and nr items from right
; rip [o index] - rips coll and returns [pre-coll item-at suf-coll]
; sew [pre-coll item-arr suf-coll] - opposite of rip, but with arr
(defn split [ind coll-0]
  (if (vector? coll-0)
      [(subvec+ coll-0 0   ind)
       (subvec+ coll-0 ind (count+ coll-0))]
      (split-at coll-0 ind)))
(defn split-with-v+ [pred coll-0] ; IMPROVE
  (->> coll-0
       (split-with pred)
       (map+ vec+)))
;_._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._
;=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*{        ASSOCIATIVE       }=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=
;=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*{                          }=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=
;___________________________________________________________________________________________________________________________________
;=================================================={          ASSOC           }=====================================================
;=================================================={ update(-in), assoc(-in)  }=====================================================
(defn- extend-coll-to
  "Extends an associative structure (for now, only vector) to a given index."
  {:attribution "Alex Gunnarson"
   :usage "USAGE: (extend-coll-to [1 2 3] 5) => [1 2 3 nil nil]"}
  [coll-0 k]
  (if (and (vector? coll-0)
           (number? k)
           (-> coll-0 count+ dec (< k)))
      (let [trans?   (transient? coll-0)
            trans-fn (if trans? identity transient)
            pers-fn  (if trans? identity persistent!)]
        (pers-fn
          (reduce+
            (fn [coll-n _] (conj! coll-n nil)) ; extend-vec part
            (trans-fn coll-0)
            (range (count+ coll-0) (inc k)))))
      coll-0))
(defn assoc+
  ([coll-0 k v]
    (ifn (extend-coll-to coll-0 k) editable?
         (fn-> transient (assoc! k v) persistent!)
         (f*n  assoc k v)))
  ([coll-0 k v & kvs-0]
    (let [edit?    (editable? coll-0)
          trans-fn (if edit? transient   identity)
          pers-fn  (if edit? persistent! identity)
          assoc-fn (if edit? assoc!      assoc)]
      (loop [kvs-n kvs-0 coll-f (-> coll-0 trans-fn (extend-coll-to k) (assoc-fn k v))]
        (if (empty? kvs-n)
            (pers-fn coll-f)
            (recur (-> kvs-n rest rest)
                   (let [k-n (first kvs-n)]
                     (-> coll-f (extend-coll-to k-n) (assoc-fn k-n (second kvs-n))))))))))
(defn update+
  "Updates the value in an associative data structure @coll associated with key @k
   by applying the function @f to the existing value."
  ^{:attribution "weavejester.medley"
    :contributors ["Alex Gunnarson"]}
  ([coll k f]      (assoc+ coll k       (f (get coll k))))
  ([coll k f args] (assoc+ coll k (apply f (get coll k) args))))
(defn updates+
  "For each key-function pair in @kfs,
   updates value in an associative data structure @coll associated with key
   by applying the function @f to the existing value."
  ^{:attribution "Alex Gunnarson"}
  ([coll & kfs]
    (reduce-2
      (fn [ret k-n f-n] (update+ ret k-n f-n))
      coll
      kfs)))
(defn update-val+
  ^{:attribution "Alex Gunnarson"}
  ([coll f] (assoc+ coll 1 (f (get coll 1)))))
;--------------------------------------------------{        UPDATE-IN         }-----------------------------------------------------
(defn update-in!
  "'Updates' a value in a nested associative structure, where ks is a sequence of keys and
  f is a function that will take the old value and any supplied args and return the new
  value, and returns a new nested structure. The associative structure can have transients
  in it, but if any levels do not exist, non-transient hash-maps will be created."
  ^{:attribution "flatland.useful"}
  [m [k & ks] f & args]
  (let [assoc-fn (if (instance? clojure.lang.ITransientCollection m) assoc! assoc)
        val (get m k)]
    (assoc-fn m k
      (if ks
          (apply update-in! val ks f args)
          (apply f val args)))))
; perhaps make a version of update-in : update :: assoc-in : assoc ?

(defn update-in+
  "Created so vectors would also automatically be grown like maps,
   given indices not present in the vector."
  ^{:attribution "Alex Gunnarson"
    :TODO ["optimize via transients"
           "allow to use :last on vectors"
           "allow |identity| function for unity's sake"]}
  [coll-0 [k0 & keys-0] v0]
  (let [value (get coll-0 k0 (when (-> keys-0 first number?) []))
        coll-f (extend-coll-to coll-0 k0)
        val-f (if keys-0
                  (update-in+ value keys-0 v0) ; make a non-stack-consuming version
                  v0)]
    (assoc coll-f k0 (whenf val-f fn? (*fn (get coll-f k0))))))
;--------------------------------------------------{         ASSOC-IN         }-----------------------------------------------------
(defn assoc-in+
  [coll ks v]
  (update-in+ coll ks (constantly v)))
(defn assoc-in!
  "Associates a value in a nested associative structure, where ks is a sequence of keys
  and v is the new value and returns a new nested structure. The associative structure
  can have transients in it, but if any levels do not exist, non-transient hash-maps will
  be created."
  ^{:attribution "flatland.useful"}
  [m ks v]
  (update-in! m ks (constantly v)))
(defn assocs-in+
  ^{:usage "(assocs-in ['file0' 'file1' 'file2']
             [0] 'file10'
             [1] 'file11'
             [2] 'file12')"}
  [coll & kvs]
  (reduce-2
    (fn [ret k-n v-n] (assoc-in+ ret k-n v-n))
    coll
    kvs))
;___________________________________________________________________________________________________________________________________
;=================================================={          DISSOC          }=====================================================
;=================================================={                          }=====================================================
(defn dissoc+
  ([coll key-0]
    (try
      (cond ; probably use tricks to see which subvec is longer to into is less consumptive
        (vector? coll)
        (catvec (subvec+ coll 0 key-0)
                (subvec+ coll (inc key-0) (count+ coll)))
        (editable? coll)
        (-> coll transient (dissoc! coll key-0) persistent!)
        :else  (dissoc coll key-0))
      (catch ClassCastException e (dissoc coll key-0)))) ; Probably because of transients...
  ([coll key-0 & keys-0]
    (reduce+ dissoc+ coll (cons key-0 keys-0))))
(defn dissocs+ [coll & ks]
  (reduce+
    (fn [ret k]
      (dissoc+ ret k))
    coll
    ks))
(defn dissoc-if+ [coll pred k] ; make dissoc-ifs+
  (whenf coll (fn-> (get k) pred)
    (f*n dissoc+ k)))
(defn dissoc-in+ ; make transient
  "Dissociate a value in a nested assocative structure, identified by a sequence
  of keys. Any collections left empty by the operation will be dissociated from
  their containing structures.
  This implementation was adapted from clojure.core.contrib"
  ^{:attribution "weavejester.medley"}
  [m ks]
  (if-let [[k & ks] (seq ks)]
    (if (seq ks)
      (let [new-n (dissoc-in+ (get m k) ks)] ; this is terrible
        (if (empty? new-n) ; dissoc's empty ones
            (dissoc m k)
            (assoc m k new-n)))
      (dissoc m k))
    m))
(defn updates-in+
  [coll & kfs]
  (reduce-2
    (fn [ret k-n f-n] (update-in+ ret k-n f-n))
    coll
    kfs))
(defn re-assoc+ [coll k-0 k-f]
  (if (contains? coll k-0)
      (-> coll
         (assoc+  k-f (get coll k-0))
         (dissoc+ k-0))
      coll))
(defn re-assocs+ [coll & kfs]
  (reduce-2
    (fn [ret k-n f-n] (re-assoc+ ret k-n f-n))
    coll
    kfs))
;___________________________________________________________________________________________________________________________________
;=================================================={   DISTINCT, INTERLEAVE   }=====================================================
;=================================================={  interpose, frequencies  }=====================================================
(defn distinct-by-java ; 201.369164 ms
  "Returns elements of coll which return unique
   values according to f. If multiple elements of coll return the same
   value under f, the first is returned"
   ; {:attribution "prismatic.plumbing"}
  [f coll]
  (let [s (java.util.HashSet.)] ; instead of #{}
    (for [x coll
         :let [id (f x)]
         :when (not (.contains s id))]
     (do (.add s id)
         x))))
(defn plicates [oper n]
  (fn [coll]
     (-> (fn [elem]
           (-> (filter+ (eq? elem) coll)
               count+
               (oper n))) ; duplicates? keep them
         (filter+ coll)
         distinct+
         fold+)))
; /interpose/
; (interpose ", " ["one" "two" "three"])
; => ("one" ", " "two" ", " "three")
; (defn interleave-all
;   "Analogy: partition:partition-all :: interleave:interleave-all"
;   {:attribution "prismatic/plumbing"}
;   [& colls]
;   (lazy-seq
;    ((fn helper [seqs]
;       (when (seq seqs)
;         (concat (map first seqs)
;                 (lazy-seq (helper (keep next seqs))))))
;     (keep seq colls))))
(defn interleave+ [& args] ; 4.307220 ms vs. 1.424329 ms normal interleave :/ because of zipvec...
  (reduce+
    (fn ([]      [])
        ([a]     (conj [] a))
        ([a b]   (conj    a b)) 
        ([a b c] (conj    a b c)))
    (apply zipvec+ args)))
; https://groups.google.com/forum/#!topic/clojure/d2lDCG3iE_k - Dubious performance of hash-map
(defn frequencies+
  "Like clojure.core/frequencies, but faster.
   Uses Java's equal/hash, so may produce incorrect results if
   given values that are = but not .equal"
  ^{:attribution "prismatic.plumbing"
    :performance "4.048617 ms vs. |frequencies| 6.341091 ms"}
  [xs]
  (let [res (java.util.HashMap.)]
    (doseq [x xs]
      (->> (.get res x)
           (or 0)
           int
           unchecked-inc)
           (.put res x))
    (into+ {} res)))
;___________________________________________________________________________________________________________________________________
;=================================================={         GROUPING         }=====================================================
;=================================================={     group, aggregate     }=====================================================
(defn group [& {:keys [coll header-pred elem-pred elem-fn header-filter]}] ; probably terrible...
  (let [result
         (loop [[elem :as coll-n] coll
                 map-f  {}
                 header-n nil]
            (if (empty? coll-n)
                map-f
                (recur (rest coll-n)
                       (cond (header-pred elem) ; the elem is a header
                             (assoc map-f (elem-fn elem) []) ; new entry
                             (elem-pred elem)
                             (assoc map-f header-n  ; add it to the current header
                               (conj (get+ map-f header-n)
                                     (elem-fn elem)))
                             :else map-f) ; otherwise leave it unchanged
                       (if (header-pred elem)
                           (elem-fn elem)
                           header-n))))
        headers (keys result)
        headers-no (filter (complement header-filter) headers)]
    (apply dissoc result headers-no)))
(defn aggregate [coll-0 start-pred take-while-pred merge-pred merge-func & [debug?]]
  (loop [coll-n coll-0
         coll-f []]
    (if ((compr last+ (fn-and vector? empty?)) coll-f)
        (apply catvec (butlast+ coll-f))
        (let [[pre-n [agg-contents :as aggregated-n] rest-n]
               (-> (split-with-v+ (complement start-pred) coll-n)
                   fold+
                   (#(conjl (fold+ (split-with-v+ take-while-pred (second %))) (first %)))
                   (#(do (when debug? (println "===========split:==========") (pprint %))
                         %))
                   (update-in [1]
                     (compr (if*n merge-pred
                                     merge-func ; merge them if they're maps
                                     identity)
                            (if*n vector? identity vector))))
              alert (when debug?
                      (println "===========pre-n:==========") (pprint pre-n)
                      (println "=======aggregated-n:=======") (pprint aggregated-n)
                      (println "==========rest-n:==========") (pprint rest-n))
              alert (when debug? (println "========coll-f:=======") (pprint coll-f))]
          (recur rest-n
                 (if (= rest-n coll-n)
                     (conj coll-f rest-n [])
                     (conj coll-f pre-n aggregated-n)))))))
;___________________________________________________________________________________________________________________________________
;=================================================={     TREE STRUCTURES      }=====================================================
;=================================================={                          }=====================================================
(defalias walk     walk/walk)
(defalias prewalk  walk/prewalk)
(defalias postwalk walk/postwalk)
(defn tree-filter
  "Like |filter|, but performs a |postwalk| on a treelike structure @tree, putting in a new vector
   only the elements for which @pred is true. Applies function @elem-func on each element."
  ^{:attribution "Alex Gunnarson"}
  [pred elem-func tree]
  (let [results (transient [])]
    (postwalk
      #(if (pred %)
           (do (conj! results (elem-func %))
               %)
           %) ; keep it the same
      tree)
    (persistent! results)))
;___________________________________________________________________________________________________________________________________
;=================================================={   COLLECTIONS CREATION   }=====================================================
;=================================================={                          }=====================================================
(defn coll-struct
  "Usage:
  (lib/coll-struct :size 5 :in [] :element {})
  => [{} {} {} {} {}]
  (lib/coll-struct :size 5 :in {} :elem-func #(hash-map (keyword (str %)) %))
  => {:4 4, :3 3, :2 2, :1 1, :0 0}"
  [& {:keys [in size element elem-func]
      :or   [elem-func identity]
      :as   args}]
  (let [elem-func-f
         (cond (nnil? element)   (fn [n] element)
               (nnil? elem-func) elem-func)]
    (loop [n 0 coll-n in]
      (if (= n size)
          coll-n
          (recur (inc n) (conj coll-n (elem-func-f n)))))))
(defn accumulate
  ^{:usage "(accumulate :decum [1 2 3] :accum () :func #(conj %1 (inc %2)))
            => (4 3 2)"}
  [& {:keys [decum accum func]
      :as args}]
  (loop [[list-n-0 & list-r :as list-n] decum
         list-f accum]
    (if (empty? list-n)
        list-f
        (recur list-r
               (func list-f list-n-0)))))
