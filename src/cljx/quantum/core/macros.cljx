(ns quantum.core.macros
  (:require
    [quantum.core.ns :as ns :refer
      #+clj [alias-ns defalias]
      #+cljs [Exception IllegalArgumentException
              Nil Bool Num ExactNum Int Decimal Key Vec Set
              ArrList TreeMap LSeq Regex Editable Transient Queue Map]]
    [quantum.core.type :as type :refer
      #+clj  [name-from-class ShortArray LongArray FloatArray IntArray DoubleArray BooleanArray ByteArray CharArray ObjectArray bigint?
              instance+? array-list? boolean? double? map-entry? sorted-map?
              queue? lseq? coll+? pattern? regex? editable? transient?]
      #+cljs [class instance+? array-list? boolean? double? map-entry? sorted-map?
              queue? lseq? coll+? pattern? regex? editable? transient?]]
    #+clj [potemkin.types :as t])
  #+clj (:gen-class))

(defmacro extend-protocol-for-all [prot classes & body]
  `(doseq [class-n# ~classes]
     (extend-protocol ~prot (eval class-n#) ~@body)))

(defmacro extend-protocol-type
  [protocol prot-type & methods]
  `(extend-protocol ~protocol ~prot-type ~@methods))
(defmacro extend-protocol-types
  [protocol prot-types & methods]
  `(doseq [prot-type# ~prot-types]
     (extend-protocol-type ~protocol (eval prot-type#) ~@methods)))

; The most general.
; (defmacro extend-protocol-typed [expr]
;   (extend-protocol (count+ [% coll] (alength % coll))))


; (defprotocol+ QBItemSearch
;   (qb-item-search-base    [search-token ^AFunction filter-fn ^AFunction comparison-fn]
;     ([String Pattern]
;       (->> qb-items*
;            (filter-fn (compr key+ (comparison-fn search-token))))))
;   (qb-item-search-compare [search-token ^AFunction filter-fn]
;     (String
;       (qb-item-search-base search-token filter-fn eq?))
;     (Pattern
;       (qb-item-search-base search-token filter-fn (partial partial str/re-find+))))
;   (qb-item-search*        [search-token]
;     ([String Pattern]
;       (qb-item-search-compare search-token filter+)))
;   (qb-item-search-first* [search-token]
;     ([String Pattern]
;       (qb-item-search-compare search-token ffilter))))

; (defmacro defprotocol+ [protocol & exprs]
;   '(let [methods# ; Just take the functions
;           (->> (rest exprs)
;                (take-while (fn->> str first+ (not= "["))))]
;      (defprotocol ~protocol
;        ~@methods#)
;      ;(extend-protocol-types protocol (first exprs) methods#)
;      ))

; (let [a# '[[bb] (fn [] cc) (fn [] dd) [ee]]]
;   (->> (rest a#)
;        (take-while (fn->> str first+ (not= "[")))
;        (map (juxt first second))))


; (defmacro quote-exprs [& exprs]
;   `~(exprs))

(defmacro ^{:private true :attribution "clojure.core, via Christophe Grand - https://gist.github.com/cgrand/5643767"}
  assert-args [& pairs]
  `(do (when-not ~(first pairs)
         (throw (IllegalArgumentException.
                  (str (first ~'&form) " requires " ~(second pairs) " in " ~'*ns* ":" (:line (meta ~'&form))))))
     ~(let [more (nnext pairs)]
        (when more
          (list* `assert-args more)))))

(defn emit-comprehension
  ^{:attribution "clojure.core, via Christophe Grand - https://gist.github.com/cgrand/5643767"}
  [&form {:keys [emit-other emit-inner]} seq-exprs body-expr]
  (assert-args
     (vector? seq-exprs) "a vector for its binding"
     (even? (count seq-exprs)) "an even number of forms in binding vector")
  (let [groups (reduce (fn [groups [k v]]
                         (if (keyword? k)
                           (conj (pop groups) (conj (peek groups) [k v]))
                           (conj groups [k v])))
                 [] (partition 2 seq-exprs)) ; /partition/... hmm...
        inner-group (peek groups)
        other-groups (pop groups)]
    (reduce emit-other (emit-inner body-expr inner-group) other-groups)))

(defn do-mod [mod-pairs cont & {:keys [skip stop]}]
  (let [err (fn [& msg] (throw (IllegalArgumentException. ^String (apply str msg))))]
    (reduce 
      (fn [cont [k v]]
        (cond 
          (= k :let)   `(let ~v ~cont)
          (= k :while) `(if  ~v ~cont ~stop)
          (= k :when)  `(if  ~v ~cont ~skip)
          :else (err "Invalid 'for' keyword " k)))
      cont (reverse mod-pairs)))) ; this is terrible
