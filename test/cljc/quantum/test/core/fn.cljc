(ns quantum.test.core.fn
  (:require [quantum.core.fn :as ns]))

(defn test:mfn
  ([macro-sym])
  ([n macro-sym]))

(defn test:call
  ([f]             )
  ([f x]           )
  ([f x y]         )
  ([f x y z]       )
  ([f x y z & more]))

(defn test:firsta
  ([x]           )
  ([x y]         )
  ([x y z]       )
  ([x y z & more]))

(defn test:seconda
  ([x y]         )
  ([x y z]       )
  ([x y z & more]))

;___________________________________________________________________________________________________________________________________
;=================================================={  HIGHER-ORDER FUNCTIONS   }====================================================
;=================================================={                           }====================================================
(defn test:do-curried
  [name doc meta args body])

(defn test:defcurried
  [name doc meta args & body])

(defn test:zeroid
  [func base])

(defn test:monoid
  [op ctor])

(defn aritoid
  ([f0         ]) 
  ([f0 f1      ]) 
  ([f0 f1 f2   ]) 
  ([f0 f1 f2 f3]))
                
(defn test:compr
  [& args])

(defn test:fn*
  [& args])

(defn test:f**n [func & args])

(defn test:*fn [& args])

(defn test:fn-bi [arg])
(defn test:unary [pred])

(defn test:fn->
  [& body])

(defn test:fn->>
  [& body])

(defn test:with-do
  [expr & exprs])

(defn test:call->  [arg & [func & args]])
(defn test:call->> [& [func & args]])

(defn test:<-
  ([x])
  ([cmd & body]))

; ---------------------------------------
; ================ JUXTS ================
; ---------------------------------------

(defn test:juxtm*
  [map-type args])

(defn test:juxtk*
  [map-type args])

(defn test:juxtm [& args])
(defn test:juxt-sm [& args])

(defn test:juxtk
  [& args])

(defn test:juxt-kv
  [kf vf])

; ======== WITH =========

(defn test:doto->>
  [f & args])

(defn test:doto-2
  [expr side])

(defn test:with-pr->>  [obj      ])
(defn test:with-msg->> [msg  obj ])
(defn test:with->>     [expr obj ])
(defn test:withf->>    [f    obj ])
(defn test:withf       [obj  f   ])
(defn test:withfs      [obj  & fs])

#?(:clj (defn test:->predicate [f]))

; ========= REDUCER PLUMBING ==========

(defn test:do-rfn
  [f1 k fkv])

(defn test:rfn
  [[f1 k] fkv])

(defn test:maybe-unary
  [f])
