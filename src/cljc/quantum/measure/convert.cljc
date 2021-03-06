(ns quantum.measure.convert
  (:require
    [quantum.core.error   :as err
      :refer [->ex throw-unless]]
    [quantum.core.collections.base
      :refer [nempty?]]
    [quantum.measure.reg  :as mreg]
    [quantum.measure.core :as meas]
    [quantum.measure.angle]
    [quantum.measure.information] ; Not too large
  #_[quantum.measure.length] ; Too large
    [quantum.measure.luminosity]
    [quantum.measure.solid-angle]
    [quantum.measure.substance]
    [quantum.measure.temperature]
    [quantum.measure.time] ; Not too large
    [quantum.measure.volume]
    [quantum.measure.weight])
  (:require-macros
    [quantum.measure.convert :as self]))

(defn assert-types [& type-pairs]
  (doseq [[unit unit-types] type-pairs]
    (throw-unless (nempty? unit-types)        (->ex "Unit not found" unit))
    (throw-unless (-> unit-types count (= 1)) (->ex "Ambiguous units found" unit-types))))

#?(:clj
(defmacro convert [n from to]
  (let [from-types (get @mreg/reg-units from)
        to-types   (get @mreg/reg-units to)
        _ (assert-types [from from-types] [to to-types])
        from-type (first from-types)
        to-type   (first to-types)
        _ (throw-unless (= from-type to-type) (->ex "Incompatible types" [from-type to-type]))
        ;conversion-fn-sym
        ;  (symbol (str "quantum.measure." (name from-type))
        ;          (str (str/str+ from "-") "->" (str/str+ to "-")))
        ;conversion-code `(~conversion-fn-sym ~n)
        conversion-map-sym (symbol (str "quantum.measure." (name from-type)) "conversion-map")
        conversion-map-var (resolve conversion-map-sym)]

    (when-not conversion-map-var (throw (->ex "Conversion map does not exist" conversion-map-sym)))
    `(* ~n ~(get-in @conversion-map-var [from to])))))
