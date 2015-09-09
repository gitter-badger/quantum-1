(ns quantum.core.reflect
  (:require-quantum [ns logic fn map]))

#?(:clj
(defn var-args?
  "Checks whether a function contains variable arguments."
  {:adapted "https://github.com/zcaudate/hara/blob/master/src/hara/function/args.clj"}
  [^clojure.lang.Fn f]
  (->> f class (.getDeclaredMethods)
       (some (fn [^java.lang.reflect.Method mthd]
          (= "getRequiredArity" (.getName mthd)))))))

(defn var-arg-count
  "Counts the number of arguments types before variable arguments."
  {:adapted "https://github.com/zcaudate/hara/blob/master/src/hara/function/args.clj"}
  [f]
  (when (var-args? f)
    (.getRequiredArity ^clojure.lang.RestFn f)))

(defn arg-count
  "Counts the number of non-varidic argument types"
  {:tests '{(fn [x])            [1]
            (fn [x & xs])       []
            (fn ([x]) ([x y]))  [1 2]}}
  {:adapted "https://github.com/zcaudate/hara/blob/master/src/hara/function/args.clj"}
  [f]
  (let [ms (filter (fn [^java.lang.reflect.Method mthd]
                     (= "invoke" (.getName mthd)))
                   (.getDeclaredMethods (class f)))
        ps (map (fn [^java.lang.reflect.Method m]
                  (.getParameterTypes m)) ms)]
    (map count ps)))

#?(:clj
(defn invoke [obj method & args]
  (clojure.lang.Reflector/invokeInstanceMethod obj method
    (into-array Object args))))

; from zcaudate/hara.reflect.types.modifiers

(def flags
  {:plain          0
   :public         1      ;; java.lang.reflect.Modifier/PUBLIC
   :private        2      ;; java.lang.reflect.Modifier/PRIVATE
   :protected      4      ;; java.lang.reflect.Modifier/PROTECTED
   :static         8      ;; java.lang.reflect.Modifier/STATIC
   :final          16     ;; java.lang.reflect.Modifier/FINAL
   :synchronized   32     ;; java.lang.reflect.Modifier/SYNCHRONIZE

   :native         256    ;; java.lang.reflect.Modifier/NATIVE
   :interface      512    ;; java.lang.reflect.Modifier/INTERFACE
   :abstract       1024   ;; java.lang.reflect.Modifier/ABSTRACT
   :strict         2048   ;; java.lang.reflect.Modifier/STRICT

   :synthetic      4096   ;; java.lang.Class/SYNTHETIC
   :annotation     8192   ;; java.lang.Class/ANNOTATION
   :enum           16384})  ;; java.lang.Class/ENUM

(def field-flags
  {:volatile       64    ;; java.lang.reflect.Modifier/VOLATILE
   :transient      128})  ;; java.lang.reflect.Modifier/TRANSIENT

(def method-flags
  {:bridge         64    ;; java.lang.reflect.Modifier/BRIDGE
   :varargs        128})    ;; java.lang.reflect.Modifier/VARARGS
   


(defn enum-values
  {:source "zcaudate/hara.object.enum"}
  [type]
  (let [vf (reflect/query-class type ["$VALUES" :#])]
    (->> (vf type) (seq))))

(defn max-inputs
  "finds the maximum number of inputs that a function can take
  
  (max-inputs (fn ([a]) ([a b])) 4)
  => 2
  (max-inputs (fn [& more]) 4)
  => 4
  
  (max-inputs (fn ([a])) 0)
  => throws"
  {:source "zcaudate/hara.concurrent.procedure"}
  [func num]
  (if (args/vargs? func)
    num
    (let [cargs (args/arg-count func)
          carr (filter #(<= % num) cargs)]
      (if (empty? carr)
        (throw (Exception. (str "Function needs at least " (apply min cargs) " inputs")))
        (apply max carr)))))

(defnt qualified-name 
  {:todo ["Different ns"]
  ([^clojure.lang.Var x]
    (str (-> x meta :ns ns-name name) "/"
         (-> x meta :name name)))
  ([^clojure.lang.Namespace x]
    (-> x ns-name name)))

(defn all-todos
  {:todo ["Put this in quantum.core.ns"]}
  []
  (->> (all-ns)
       (map (juxt (juxt identity)
            (fn-> ns-name ns-interns vals)))
       (apply concat) ; flatten-1
       (apply concat)
       (map (juxt identity (fn-> meta :todo)))
       (remove (fn-> second empty?))
       (into (map/sorted-map-by
               (fn [a b] (compare (qualified-name a)
                                  (qualified-name b)))))))


