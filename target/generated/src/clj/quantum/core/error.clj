(ns quantum.core.error
  (:require
    [quantum.core.ns :as ns :refer
            [alias-ns defalias]
                                                
                                                           
                                                                      ]  
          [slingshot.slingshot :as try-catch])
       
  (:import
    clojure.core.Vec
    (quantum.core.ns
      Nil Bool Num ExactNum Int Decimal Key Set
             ArrList TreeMap LSeq Regex Editable Transient Queue Map))
        (:gen-class))

      (defalias try+   try-catch/try+)
      (defalias throw+ try-catch/throw+)

                                      

; NEED MORE MACRO EXPERIENCE TO DO THIS
; (defmacro catch-or
;   "Like /catch/, but catches multiple given exceptions in the same way."
;   {:in "[[[:status 401] {:keys [status]}]
;          [[:status 403] {:keys [status]}]
;          [[:status 500] {:keys [status]}]]
;         (handle-http-error status)"}
;   [exception-keys-pairs func]
;   (for [[exception-n# keys-n#] exception-keys-pairs]
;      `(catch exception-n# keys-n# func)))

(defn with-throw
  "Throws an exception with the given message @message if
   @expr evaluates to false.

   Specifically for use with :pre and :post conditions."
  [expr throw-content]
  `(if ~expr ~expr (throw+ ~throw-content)))

; ====== ERROR TYPES =======

(defn unk-dispatch [dispatch]
  {:type :unk-dispatch
   :message (str "Unknown dispatch function '" (name dispatch) "' requested.")})
(defn unk-key [k]
  {:type :unk-key
   :message (str "Unknown dispatch key '"      (name k)        "' requested.")})

;;;;;;;;;;;; This file autogenerated from src/cljx/quantum/core/error.cljx
