(ns quantum.measure.time
  (:require-quantum [ns fn map logic])
  (:require quantum.measure.reg)
  #_(:require [quantum.measure.core :refer [defunits-of emit-defunits-code]]))

#?(:clj (set! *unchecked-math* false))

(def ^:const planck-time-constant (rationalize 5.39106E-44))

#_(defunits-of time [:seconds #{:sec :s}]
  ; A second is a duration of 9192631770 periods of the radiation
  ; corresponding to the transition between the two hyperfine
  ; levels of the ground state of the cesium-133 atom
  ; Microscopic
  :planck-quanta [[planck-time-constant :sec   ]]
  :yoctos        [[1/1000               :zeptos] nil #{:ys  :yoctoseconds}]
  :zeptos        [[1/1000               :attos ] nil #{:zs  :zeptoseconds}]
  :attos         [[1/1000               :femtos] nil #{:as  :attoseconds}]
  :femtos        [[1/1000               :picos ] nil #{:fs  :femtoseconds}]
  :picos         [[1/1000               :nanos ] nil #{:ps  :picoseconds}]
  :nanos         [[1/1000               :micros] nil #{:ns  :nanoseconds}]
  :micros        [[1/1000               :millis] nil #{:mcs :microseconds :µs}]
  :millis        [[1/1000               :sec   ] nil #{:ms  :milliseconds}]
  ; Macroscopic               
  :min           [[60                   :sec   ] #{:minutes} #{ :m}]
  :hrs           [[60                   :min   ] #{:hours}]
  :days          [[24                   :hrs   ] nil #{:d :julian-days}]
  :weeks         [[7                    :days  ] #{:wks} #{:sennights}]
  :months        [[1/12                 :years ] #{:mos}]
  :fortnights    [[14                   :days  ]]
  :common-years  [[365                  :days  ]]
  :years         [[365.25               :days  ] #{:yrs} #{:julian-years}]
  :leap-years    [[366                  :days  ]]
  :decades       [[10                   :years ]]
  :centuries     [[100                  :years ]]
  :millennia     [[1000                 :years ] nil #{:megayears}])

; PRECOMPILED DUE TO COMPILER LIMITATIONS ("Method code too large!" error)

(swap!
 quantum.measure.reg/reg-units
 (fn
  [u]
  (reduce
   (fn [ret [k f]] (update ret k f))
   u
   (->>
    #{:attos
      :centuries
      :common-years
      :days
      :decades
      :femtos
      :fortnights
      :hours
      :hrs
      :leap-years
      :micros
      :millennia
      :millis
      :min
      :minutes
      :months
      :mos
      :nanos
      :picos
      :planck-quanta
      :sec
      :weeks
      :wks
      :years
      :yoctos
      :yrs
      :zeptos}
    (map
     (fn
      [node]
      (map-entry
       node
       (if
        (get quantum.measure.reg/reg-units node)
        (f*n conj :time)
        (constantly #{:time})))))))))

(defn attos->centuries [n] (* n 1/3155760000000000000000000000))
(defn attos->common-years [n] (* n 1/31536000000000000000000000))
(defn attos->days [n] (* n 1/86400000000000000000000))
(defn attos->decades [n] (* n 1/315576000000000000000000000))
(defn attos->femtos [n] (* n 1/1000))
(defn attos->fortnights [n] (* n 1/1209600000000000000000000))
(defn attos->hours [n] (* n 1/3600000000000000000000))
(defn attos->hrs [n] (* n 1/3600000000000000000000))
(defn attos->leap-years [n] (* n 1/31622400000000000000000000))
(defn attos->micros [n] (* n 1/1000000000000))
(defn attos->millennia [n] (* n 1/31557600000000000000000000000))
(defn attos->millis [n] (* n 1/1000000000000000))
(defn attos->min [n] (* n 1/60000000000000000000))
(defn attos->minutes [n] (* n 1/60000000000000000000))
(defn attos->months [n] (* n 1/2629800000000000000000000))
(defn attos->mos [n] (* n 1/2629800000000000000000000))
(defn attos->nanos [n] (* n 1/1000000000))
(defn attos->picos [n] (* n 1/1000000))
(defn attos->planck-quanta [n] (* n 5000000000000000000000000000000/269553))
(defn attos->sec [n] (* n 1/1000000000000000000))
(defn attos->weeks [n] (* n 1/604800000000000000000000))
(defn attos->wks [n] (* n 1/604800000000000000000000))
(defn attos->years [n] (* n 1/31557600000000000000000000))
(defn attos->yoctos [n] (* n 1000000N))
(defn attos->yrs [n] (* n 1/31557600000000000000000000))
(defn attos->zeptos [n] (* n 1000N))
(defn centuries->attos [n] (* n 3155760000000000000000000000N))
(defn centuries->common-years [n] (* n 7305/73))
(defn centuries->days [n] (* n 36525N))
(defn centuries->decades [n] (* n 10N))
(defn centuries->femtos [n] (* n 3155760000000000000000000N))
(defn centuries->fortnights [n] (* n 36525/14))
(defn centuries->hours [n] (* n 876600N))
(defn centuries->hrs [n] (* n 876600N))
(defn centuries->leap-years [n] (* n 12175/122))
(defn centuries->micros [n] (* n 3155760000000000N))
(defn centuries->millennia [n] (* n 1/10))
(defn centuries->millis [n] (* n 3155760000000N))
(defn centuries->min [n] (* n 52596000N))
(defn centuries->minutes [n] (* n 52596000N))
(defn centuries->months [n] (* n 1200N))
(defn centuries->mos [n] (* n 1200N))
(defn centuries->nanos [n] (* n 3155760000000000000N))
(defn centuries->picos [n] (* n 3155760000000000000000N))
(defn centuries->planck-quanta [n] (* n 5259600000000000000000000000000000000000000000000000000000/89851))
(defn centuries->sec [n] (* n 3155760000N))
(defn centuries->weeks [n] (* n 36525/7))
(defn centuries->wks [n] (* n 36525/7))
(defn centuries->years [n] (* n 100N))
(defn centuries->yoctos [n] (* n 3155760000000000000000000000000000N))
(defn centuries->yrs [n] (* n 100N))
(defn centuries->zeptos [n] (* n 3155760000000000000000000000000N))
(defn common-years->attos [n] (* n 31536000000000000000000000N))
(defn common-years->centuries [n] (* n 73/7305))
(defn common-years->days [n] (* n 365N))
(defn common-years->decades [n] (* n 146/1461))
(defn common-years->femtos [n] (* n 31536000000000000000000N))
(defn common-years->fortnights [n] (* n 365/14))
(defn common-years->hours [n] (* n 8760N))
(defn common-years->hrs [n] (* n 8760N))
(defn common-years->leap-years [n] (* n 365/366))
(defn common-years->micros [n] (* n 31536000000000N))
(defn common-years->millennia [n] (* n 73/73050))
(defn common-years->millis [n] (* n 31536000000N))
(defn common-years->min [n] (* n 525600N))
(defn common-years->minutes [n] (* n 525600N))
(defn common-years->months [n] (* n 5840/487))
(defn common-years->mos [n] (* n 5840/487))
(defn common-years->nanos [n] (* n 31536000000000000N))
(defn common-years->picos [n] (* n 31536000000000000000N))
(defn common-years->planck-quanta [n] (* n 52560000000000000000000000000000000000000000000000000000/89851))
(defn common-years->sec [n] (* n 31536000N))
(defn common-years->weeks [n] (* n 365/7))
(defn common-years->wks [n] (* n 365/7))
(defn common-years->years [n] (* n 1460/1461))
(defn common-years->yoctos [n] (* n 31536000000000000000000000000000N))
(defn common-years->yrs [n] (* n 1460/1461))
(defn common-years->zeptos [n] (* n 31536000000000000000000000000N))
(defn days->attos [n] (* n 86400000000000000000000N))
(defn days->centuries [n] (* n 1/36525))
(defn days->common-years [n] (* n 1/365))
(defn days->decades [n] (* n 2/7305))
(defn days->femtos [n] (* n 86400000000000000000N))
(defn days->fortnights [n] (* n 1/14))
(defn days->hours [n] (* n 24N))
(defn days->hrs [n] (* n 24N))
(defn days->leap-years [n] (* n 1/366))
(defn days->micros [n] (* n 86400000000N))
(defn days->millennia [n] (* n 1/365250))
(defn days->millis [n] (* n 86400000N))
(defn days->min [n] (* n 1440N))
(defn days->minutes [n] (* n 1440N))
(defn days->months [n] (* n 16/487))
(defn days->mos [n] (* n 16/487))
(defn days->nanos [n] (* n 86400000000000N))
(defn days->picos [n] (* n 86400000000000000N))
(defn days->planck-quanta [n] (* n 144000000000000000000000000000000000000000000000000000/89851))
(defn days->sec [n] (* n 86400N))
(defn days->weeks [n] (* n 1/7))
(defn days->wks [n] (* n 1/7))
(defn days->years [n] (* n 4/1461))
(defn days->yoctos [n] (* n 86400000000000000000000000000N))
(defn days->yrs [n] (* n 4/1461))
(defn days->zeptos [n] (* n 86400000000000000000000000N))
(defn decades->attos [n] (* n 315576000000000000000000000N))
(defn decades->centuries [n] (* n 1/10))
(defn decades->common-years [n] (* n 1461/146))
(defn decades->days [n] (* n 7305/2))
(defn decades->femtos [n] (* n 315576000000000000000000N))
(defn decades->fortnights [n] (* n 7305/28))
(defn decades->hours [n] (* n 87660N))
(defn decades->hrs [n] (* n 87660N))
(defn decades->leap-years [n] (* n 2435/244))
(defn decades->micros [n] (* n 315576000000000N))
(defn decades->millennia [n] (* n 1/100))
(defn decades->millis [n] (* n 315576000000N))
(defn decades->min [n] (* n 5259600N))
(defn decades->minutes [n] (* n 5259600N))
(defn decades->months [n] (* n 120N))
(defn decades->mos [n] (* n 120N))
(defn decades->nanos [n] (* n 315576000000000000N))
(defn decades->picos [n] (* n 315576000000000000000N))
(defn decades->planck-quanta [n] (* n 525960000000000000000000000000000000000000000000000000000/89851))
(defn decades->sec [n] (* n 315576000N))
(defn decades->weeks [n] (* n 7305/14))
(defn decades->wks [n] (* n 7305/14))
(defn decades->years [n] (* n 10N))
(defn decades->yoctos [n] (* n 315576000000000000000000000000000N))
(defn decades->yrs [n] (* n 10N))
(defn decades->zeptos [n] (* n 315576000000000000000000000000N))
(defn femtos->attos [n] (* n 1000N))
(defn femtos->centuries [n] (* n 1/3155760000000000000000000))
(defn femtos->common-years [n] (* n 1/31536000000000000000000))
(defn femtos->days [n] (* n 1/86400000000000000000))
(defn femtos->decades [n] (* n 1/315576000000000000000000))
(defn femtos->fortnights [n] (* n 1/1209600000000000000000))
(defn femtos->hours [n] (* n 1/3600000000000000000))
(defn femtos->hrs [n] (* n 1/3600000000000000000))
(defn femtos->leap-years [n] (* n 1/31622400000000000000000))
(defn femtos->micros [n] (* n 1/1000000000))
(defn femtos->millennia [n] (* n 1/31557600000000000000000000))
(defn femtos->millis [n] (* n 1/1000000000000))
(defn femtos->min [n] (* n 1/60000000000000000))
(defn femtos->minutes [n] (* n 1/60000000000000000))
(defn femtos->months [n] (* n 1/2629800000000000000000))
(defn femtos->mos [n] (* n 1/2629800000000000000000))
(defn femtos->nanos [n] (* n 1/1000000))
(defn femtos->picos [n] (* n 1/1000))
(defn femtos->planck-quanta [n] (* n 5000000000000000000000000000000000/269553))
(defn femtos->sec [n] (* n 1/1000000000000000))
(defn femtos->weeks [n] (* n 1/604800000000000000000))
(defn femtos->wks [n] (* n 1/604800000000000000000))
(defn femtos->years [n] (* n 1/31557600000000000000000))
(defn femtos->yoctos [n] (* n 1000000000N))
(defn femtos->yrs [n] (* n 1/31557600000000000000000))
(defn femtos->zeptos [n] (* n 1000000N))
(defn fortnights->attos [n] (* n 1209600000000000000000000N))
(defn fortnights->centuries [n] (* n 14/36525))
(defn fortnights->common-years [n] (* n 14/365))
(defn fortnights->days [n] (* n 14N))
(defn fortnights->decades [n] (* n 28/7305))
(defn fortnights->femtos [n] (* n 1209600000000000000000N))
(defn fortnights->hours [n] (* n 336N))
(defn fortnights->hrs [n] (* n 336N))
(defn fortnights->leap-years [n] (* n 7/183))
(defn fortnights->micros [n] (* n 1209600000000N))
(defn fortnights->millennia [n] (* n 7/182625))
(defn fortnights->millis [n] (* n 1209600000N))
(defn fortnights->min [n] (* n 20160N))
(defn fortnights->minutes [n] (* n 20160N))
(defn fortnights->months [n] (* n 224/487))
(defn fortnights->mos [n] (* n 224/487))
(defn fortnights->nanos [n] (* n 1209600000000000N))
(defn fortnights->picos [n] (* n 1209600000000000000N))
(defn fortnights->planck-quanta [n] (* n 2016000000000000000000000000000000000000000000000000000/89851))
(defn fortnights->sec [n] (* n 1209600N))
(defn fortnights->weeks [n] (* n 2N))
(defn fortnights->wks [n] (* n 2N))
(defn fortnights->years [n] (* n 56/1461))
(defn fortnights->yoctos [n] (* n 1209600000000000000000000000000N))
(defn fortnights->yrs [n] (* n 56/1461))
(defn fortnights->zeptos [n] (* n 1209600000000000000000000000N))
(defn hours->attos [n] (* n 3600000000000000000000N))
(defn hours->centuries [n] (* n 1/876600))
(defn hours->common-years [n] (* n 1/8760))
(defn hours->days [n] (* n 1/24))
(defn hours->decades [n] (* n 1/87660))
(defn hours->femtos [n] (* n 3600000000000000000N))
(defn hours->fortnights [n] (* n 1/336))
(defn hours->hrs [n] n)
(defn hours->leap-years [n] (* n 1/8784))
(defn hours->micros [n] (* n 3600000000N))
(defn hours->millennia [n] (* n 1/8766000))
(defn hours->millis [n] (* n 3600000N))
(defn hours->min [n] (* n 60N))
(defn hours->minutes [n] (* n 60N))
(defn hours->months [n] (* n 2/1461))
(defn hours->mos [n] (* n 2/1461))
(defn hours->nanos [n] (* n 3600000000000N))
(defn hours->picos [n] (* n 3600000000000000N))
(defn hours->planck-quanta [n] (* n 6000000000000000000000000000000000000000000000000000/89851))
(defn hours->sec [n] (* n 3600N))
(defn hours->weeks [n] (* n 1/168))
(defn hours->wks [n] (* n 1/168))
(defn hours->years [n] (* n 1/8766))
(defn hours->yoctos [n] (* n 3600000000000000000000000000N))
(defn hours->yrs [n] (* n 1/8766))
(defn hours->zeptos [n] (* n 3600000000000000000000000N))
(defn hrs->attos [n] (* n 3600000000000000000000N))
(defn hrs->centuries [n] (* n 1/876600))
(defn hrs->common-years [n] (* n 1/8760))
(defn hrs->days [n] (* n 1/24))
(defn hrs->decades [n] (* n 1/87660))
(defn hrs->femtos [n] (* n 3600000000000000000N))
(defn hrs->fortnights [n] (* n 1/336))
(defn hrs->hours [n] n)
(defn hrs->leap-years [n] (* n 1/8784))
(defn hrs->micros [n] (* n 3600000000N))
(defn hrs->millennia [n] (* n 1/8766000))
(defn hrs->millis [n] (* n 3600000N))
(defn hrs->min [n] (* n 60N))
(defn hrs->minutes [n] (* n 60N))
(defn hrs->months [n] (* n 2/1461))
(defn hrs->mos [n] (* n 2/1461))
(defn hrs->nanos [n] (* n 3600000000000N))
(defn hrs->picos [n] (* n 3600000000000000N))
(defn hrs->planck-quanta [n] (* n 6000000000000000000000000000000000000000000000000000/89851))
(defn hrs->sec [n] (* n 3600N))
(defn hrs->weeks [n] (* n 1/168))
(defn hrs->wks [n] (* n 1/168))
(defn hrs->years [n] (* n 1/8766))
(defn hrs->yoctos [n] (* n 3600000000000000000000000000N))
(defn hrs->yrs [n] (* n 1/8766))
(defn hrs->zeptos [n] (* n 3600000000000000000000000N))
(defn leap-years->attos [n] (* n 31622400000000000000000000N))
(defn leap-years->centuries [n] (* n 122/12175))
(defn leap-years->common-years [n] (* n 366/365))
(defn leap-years->days [n] (* n 366N))
(defn leap-years->decades [n] (* n 244/2435))
(defn leap-years->femtos [n] (* n 31622400000000000000000N))
(defn leap-years->fortnights [n] (* n 183/7))
(defn leap-years->hours [n] (* n 8784N))
(defn leap-years->hrs [n] (* n 8784N))
(defn leap-years->micros [n] (* n 31622400000000N))
(defn leap-years->millennia [n] (* n 61/60875))
(defn leap-years->millis [n] (* n 31622400000N))
(defn leap-years->min [n] (* n 527040N))
(defn leap-years->minutes [n] (* n 527040N))
(defn leap-years->months [n] (* n 5856/487))
(defn leap-years->mos [n] (* n 5856/487))
(defn leap-years->nanos [n] (* n 31622400000000000N))
(defn leap-years->picos [n] (* n 31622400000000000000N))
(defn leap-years->planck-quanta [n] (* n 52704000000000000000000000000000000000000000000000000000/89851))
(defn leap-years->sec [n] (* n 31622400N))
(defn leap-years->weeks [n] (* n 366/7))
(defn leap-years->wks [n] (* n 366/7))
(defn leap-years->years [n] (* n 488/487))
(defn leap-years->yoctos [n] (* n 31622400000000000000000000000000N))
(defn leap-years->yrs [n] (* n 488/487))
(defn leap-years->zeptos [n] (* n 31622400000000000000000000000N))
(defn micros->attos [n] (* n 1000000000000N))
(defn micros->centuries [n] (* n 1/3155760000000000))
(defn micros->common-years [n] (* n 1/31536000000000))
(defn micros->days [n] (* n 1/86400000000))
(defn micros->decades [n] (* n 1/315576000000000))
(defn micros->femtos [n] (* n 1000000000N))
(defn micros->fortnights [n] (* n 1/1209600000000))
(defn micros->hours [n] (* n 1/3600000000))
(defn micros->hrs [n] (* n 1/3600000000))
(defn micros->leap-years [n] (* n 1/31622400000000))
(defn micros->millennia [n] (* n 1/31557600000000000))
(defn micros->millis [n] (* n 1/1000))
(defn micros->min [n] (* n 1/60000000))
(defn micros->minutes [n] (* n 1/60000000))
(defn micros->months [n] (* n 1/2629800000000))
(defn micros->mos [n] (* n 1/2629800000000))
(defn micros->nanos [n] (* n 1000N))
(defn micros->picos [n] (* n 1000000N))
(defn micros->planck-quanta [n] (* n 5000000000000000000000000000000000000000000/269553))
(defn micros->sec [n] (* n 1/1000000))
(defn micros->weeks [n] (* n 1/604800000000))
(defn micros->wks [n] (* n 1/604800000000))
(defn micros->years [n] (* n 1/31557600000000))
(defn micros->yoctos [n] (* n 1000000000000000000N))
(defn micros->yrs [n] (* n 1/31557600000000))
(defn micros->zeptos [n] (* n 1000000000000000N))
(defn millennia->attos [n] (* n 31557600000000000000000000000N))
(defn millennia->centuries [n] (* n 10N))
(defn millennia->common-years [n] (* n 73050/73))
(defn millennia->days [n] (* n 365250N))
(defn millennia->decades [n] (* n 100N))
(defn millennia->femtos [n] (* n 31557600000000000000000000N))
(defn millennia->fortnights [n] (* n 182625/7))
(defn millennia->hours [n] (* n 8766000N))
(defn millennia->hrs [n] (* n 8766000N))
(defn millennia->leap-years [n] (* n 60875/61))
(defn millennia->micros [n] (* n 31557600000000000N))
(defn millennia->millis [n] (* n 31557600000000N))
(defn millennia->min [n] (* n 525960000N))
(defn millennia->minutes [n] (* n 525960000N))
(defn millennia->months [n] (* n 12000N))
(defn millennia->mos [n] (* n 12000N))
(defn millennia->nanos [n] (* n 31557600000000000000N))
(defn millennia->picos [n] (* n 31557600000000000000000N))
(defn millennia->planck-quanta [n] (* n 52596000000000000000000000000000000000000000000000000000000/89851))
(defn millennia->sec [n] (* n 31557600000N))
(defn millennia->weeks [n] (* n 365250/7))
(defn millennia->wks [n] (* n 365250/7))
(defn millennia->years [n] (* n 1000N))
(defn millennia->yoctos [n] (* n 31557600000000000000000000000000000N))
(defn millennia->yrs [n] (* n 1000N))
(defn millennia->zeptos [n] (* n 31557600000000000000000000000000N))
(defn millis->attos [n] (* n 1000000000000000N))
(defn millis->centuries [n] (* n 1/3155760000000))
(defn millis->common-years [n] (* n 1/31536000000))
(defn millis->days [n] (* n 1/86400000))
(defn millis->decades [n] (* n 1/315576000000))
(defn millis->femtos [n] (* n 1000000000000N))
(defn millis->fortnights [n] (* n 1/1209600000))
(defn millis->hours [n] (* n 1/3600000))
(defn millis->hrs [n] (* n 1/3600000))
(defn millis->leap-years [n] (* n 1/31622400000))
(defn millis->micros [n] (* n 1000N))
(defn millis->millennia [n] (* n 1/31557600000000))
(defn millis->min [n] (* n 1/60000))
(defn millis->minutes [n] (* n 1/60000))
(defn millis->months [n] (* n 1/2629800000))
(defn millis->mos [n] (* n 1/2629800000))
(defn millis->nanos [n] (* n 1000000N))
(defn millis->picos [n] (* n 1000000000N))
(defn millis->planck-quanta [n] (* n 5000000000000000000000000000000000000000000000/269553))
(defn millis->sec [n] (* n 1/1000))
(defn millis->weeks [n] (* n 1/604800000))
(defn millis->wks [n] (* n 1/604800000))
(defn millis->years [n] (* n 1/31557600000))
(defn millis->yoctos [n] (* n 1000000000000000000000N))
(defn millis->yrs [n] (* n 1/31557600000))
(defn millis->zeptos [n] (* n 1000000000000000000N))
(defn min->attos [n] (* n 60000000000000000000N))
(defn min->centuries [n] (* n 1/52596000))
(defn min->common-years [n] (* n 1/525600))
(defn min->days [n] (* n 1/1440))
(defn min->decades [n] (* n 1/5259600))
(defn min->femtos [n] (* n 60000000000000000N))
(defn min->fortnights [n] (* n 1/20160))
(defn min->hours [n] (* n 1/60))
(defn min->hrs [n] (* n 1/60))
(defn min->leap-years [n] (* n 1/527040))
(defn min->micros [n] (* n 60000000N))
(defn min->millennia [n] (* n 1/525960000))
(defn min->millis [n] (* n 60000N))
(defn min->minutes [n] n)
(defn min->months [n] (* n 1/43830))
(defn min->mos [n] (* n 1/43830))
(defn min->nanos [n] (* n 60000000000N))
(defn min->picos [n] (* n 60000000000000N))
(defn min->planck-quanta [n] (* n 100000000000000000000000000000000000000000000000000/89851))
(defn min->sec [n] (* n 60N))
(defn min->weeks [n] (* n 1/10080))
(defn min->wks [n] (* n 1/10080))
(defn min->years [n] (* n 1/525960))
(defn min->yoctos [n] (* n 60000000000000000000000000N))
(defn min->yrs [n] (* n 1/525960))
(defn min->zeptos [n] (* n 60000000000000000000000N))
(defn minutes->attos [n] (* n 60000000000000000000N))
(defn minutes->centuries [n] (* n 1/52596000))
(defn minutes->common-years [n] (* n 1/525600))
(defn minutes->days [n] (* n 1/1440))
(defn minutes->decades [n] (* n 1/5259600))
(defn minutes->femtos [n] (* n 60000000000000000N))
(defn minutes->fortnights [n] (* n 1/20160))
(defn minutes->hours [n] (* n 1/60))
(defn minutes->hrs [n] (* n 1/60))
(defn minutes->leap-years [n] (* n 1/527040))
(defn minutes->micros [n] (* n 60000000N))
(defn minutes->millennia [n] (* n 1/525960000))
(defn minutes->millis [n] (* n 60000N))
(defn minutes->min [n] n)
(defn minutes->months [n] (* n 1/43830))
(defn minutes->mos [n] (* n 1/43830))
(defn minutes->nanos [n] (* n 60000000000N))
(defn minutes->picos [n] (* n 60000000000000N))
(defn minutes->planck-quanta [n] (* n 100000000000000000000000000000000000000000000000000/89851))
(defn minutes->sec [n] (* n 60N))
(defn minutes->weeks [n] (* n 1/10080))
(defn minutes->wks [n] (* n 1/10080))
(defn minutes->years [n] (* n 1/525960))
(defn minutes->yoctos [n] (* n 60000000000000000000000000N))
(defn minutes->yrs [n] (* n 1/525960))
(defn minutes->zeptos [n] (* n 60000000000000000000000N))
(defn months->attos [n] (* n 2629800000000000000000000N))
(defn months->centuries [n] (* n 1/1200))
(defn months->common-years [n] (* n 487/5840))
(defn months->days [n] (* n 487/16))
(defn months->decades [n] (* n 1/120))
(defn months->femtos [n] (* n 2629800000000000000000N))
(defn months->fortnights [n] (* n 487/224))
(defn months->hours [n] (* n 1461/2))
(defn months->hrs [n] (* n 1461/2))
(defn months->leap-years [n] (* n 487/5856))
(defn months->micros [n] (* n 2629800000000N))
(defn months->millennia [n] (* n 1/12000))
(defn months->millis [n] (* n 2629800000N))
(defn months->min [n] (* n 43830N))
(defn months->minutes [n] (* n 43830N))
(defn months->mos [n] n)
(defn months->nanos [n] (* n 2629800000000000N))
(defn months->picos [n] (* n 2629800000000000000N))
(defn months->planck-quanta [n] (* n 4383000000000000000000000000000000000000000000000000000/89851))
(defn months->sec [n] (* n 2629800N))
(defn months->weeks [n] (* n 487/112))
(defn months->wks [n] (* n 487/112))
(defn months->years [n] (* n 1/12))
(defn months->yoctos [n] (* n 2629800000000000000000000000000N))
(defn months->yrs [n] (* n 1/12))
(defn months->zeptos [n] (* n 2629800000000000000000000000N))
(defn mos->attos [n] (* n 2629800000000000000000000N))
(defn mos->centuries [n] (* n 1/1200))
(defn mos->common-years [n] (* n 487/5840))
(defn mos->days [n] (* n 487/16))
(defn mos->decades [n] (* n 1/120))
(defn mos->femtos [n] (* n 2629800000000000000000N))
(defn mos->fortnights [n] (* n 487/224))
(defn mos->hours [n] (* n 1461/2))
(defn mos->hrs [n] (* n 1461/2))
(defn mos->leap-years [n] (* n 487/5856))
(defn mos->micros [n] (* n 2629800000000N))
(defn mos->millennia [n] (* n 1/12000))
(defn mos->millis [n] (* n 2629800000N))
(defn mos->min [n] (* n 43830N))
(defn mos->minutes [n] (* n 43830N))
(defn mos->months [n] n)
(defn mos->nanos [n] (* n 2629800000000000N))
(defn mos->picos [n] (* n 2629800000000000000N))
(defn mos->planck-quanta [n] (* n 4383000000000000000000000000000000000000000000000000000/89851))
(defn mos->sec [n] (* n 2629800N))
(defn mos->weeks [n] (* n 487/112))
(defn mos->wks [n] (* n 487/112))
(defn mos->years [n] (* n 1/12))
(defn mos->yoctos [n] (* n 2629800000000000000000000000000N))
(defn mos->yrs [n] (* n 1/12))
(defn mos->zeptos [n] (* n 2629800000000000000000000000N))
(defn nanos->attos [n] (* n 1000000000N))
(defn nanos->centuries [n] (* n 1/3155760000000000000))
(defn nanos->common-years [n] (* n 1/31536000000000000))
(defn nanos->days [n] (* n 1/86400000000000))
(defn nanos->decades [n] (* n 1/315576000000000000))
(defn nanos->femtos [n] (* n 1000000N))
(defn nanos->fortnights [n] (* n 1/1209600000000000))
(defn nanos->hours [n] (* n 1/3600000000000))
(defn nanos->hrs [n] (* n 1/3600000000000))
(defn nanos->leap-years [n] (* n 1/31622400000000000))
(defn nanos->micros [n] (* n 1/1000))
(defn nanos->millennia [n] (* n 1/31557600000000000000))
(defn nanos->millis [n] (* n 1/1000000))
(defn nanos->min [n] (* n 1/60000000000))
(defn nanos->minutes [n] (* n 1/60000000000))
(defn nanos->months [n] (* n 1/2629800000000000))
(defn nanos->mos [n] (* n 1/2629800000000000))
(defn nanos->picos [n] (* n 1000N))
(defn nanos->planck-quanta [n] (* n 5000000000000000000000000000000000000000/269553))
(defn nanos->sec [n] (* n 1/1000000000))
(defn nanos->weeks [n] (* n 1/604800000000000))
(defn nanos->wks [n] (* n 1/604800000000000))
(defn nanos->years [n] (* n 1/31557600000000000))
(defn nanos->yoctos [n] (* n 1000000000000000N))
(defn nanos->yrs [n] (* n 1/31557600000000000))
(defn nanos->zeptos [n] (* n 1000000000000N))
(defn picos->attos [n] (* n 1000000N))
(defn picos->centuries [n] (* n 1/3155760000000000000000))
(defn picos->common-years [n] (* n 1/31536000000000000000))
(defn picos->days [n] (* n 1/86400000000000000))
(defn picos->decades [n] (* n 1/315576000000000000000))
(defn picos->femtos [n] (* n 1000N))
(defn picos->fortnights [n] (* n 1/1209600000000000000))
(defn picos->hours [n] (* n 1/3600000000000000))
(defn picos->hrs [n] (* n 1/3600000000000000))
(defn picos->leap-years [n] (* n 1/31622400000000000000))
(defn picos->micros [n] (* n 1/1000000))
(defn picos->millennia [n] (* n 1/31557600000000000000000))
(defn picos->millis [n] (* n 1/1000000000))
(defn picos->min [n] (* n 1/60000000000000))
(defn picos->minutes [n] (* n 1/60000000000000))
(defn picos->months [n] (* n 1/2629800000000000000))
(defn picos->mos [n] (* n 1/2629800000000000000))
(defn picos->nanos [n] (* n 1/1000))
(defn picos->planck-quanta [n] (* n 5000000000000000000000000000000000000/269553))
(defn picos->sec [n] (* n 1/1000000000000))
(defn picos->weeks [n] (* n 1/604800000000000000))
(defn picos->wks [n] (* n 1/604800000000000000))
(defn picos->years [n] (* n 1/31557600000000000000))
(defn picos->yoctos [n] (* n 1000000000000N))
(defn picos->yrs [n] (* n 1/31557600000000000000))
(defn picos->zeptos [n] (* n 1000000000N))
(defn planck-quanta->attos [n] (* n 269553/5000000000000000000000000000000))
(defn planck-quanta->centuries [n] (* n 89851/5259600000000000000000000000000000000000000000000000000000))
(defn planck-quanta->common-years [n] (* n 89851/52560000000000000000000000000000000000000000000000000000))
(defn planck-quanta->days [n] (* n 89851/144000000000000000000000000000000000000000000000000000))
(defn planck-quanta->decades [n] (* n 89851/525960000000000000000000000000000000000000000000000000000))
(defn planck-quanta->femtos [n] (* n 269553/5000000000000000000000000000000000))
(defn planck-quanta->fortnights [n] (* n 89851/2016000000000000000000000000000000000000000000000000000))
(defn planck-quanta->hours [n] (* n 89851/6000000000000000000000000000000000000000000000000000))
(defn planck-quanta->hrs [n] (* n 89851/6000000000000000000000000000000000000000000000000000))
(defn planck-quanta->leap-years [n] (* n 89851/52704000000000000000000000000000000000000000000000000000))
(defn planck-quanta->micros [n] (* n 269553/5000000000000000000000000000000000000000000))
(defn planck-quanta->millennia [n] (* n 89851/52596000000000000000000000000000000000000000000000000000000))
(defn planck-quanta->millis [n] (* n 269553/5000000000000000000000000000000000000000000000))
(defn planck-quanta->min [n] (* n 89851/100000000000000000000000000000000000000000000000000))
(defn planck-quanta->minutes [n] (* n 89851/100000000000000000000000000000000000000000000000000))
(defn planck-quanta->months [n] (* n 89851/4383000000000000000000000000000000000000000000000000000))
(defn planck-quanta->mos [n] (* n 89851/4383000000000000000000000000000000000000000000000000000))
(defn planck-quanta->nanos [n] (* n 269553/5000000000000000000000000000000000000000))
(defn planck-quanta->picos [n] (* n 269553/5000000000000000000000000000000000000))
(defn planck-quanta->sec [n] (* n 269553/5000000000000000000000000000000000000000000000000))
(defn planck-quanta->weeks [n] (* n 89851/1008000000000000000000000000000000000000000000000000000))
(defn planck-quanta->wks [n] (* n 89851/1008000000000000000000000000000000000000000000000000000))
(defn planck-quanta->years [n] (* n 89851/52596000000000000000000000000000000000000000000000000000))
(defn planck-quanta->yoctos [n] (* n 269553/5000000000000000000000000))
(defn planck-quanta->yrs [n] (* n 89851/52596000000000000000000000000000000000000000000000000000))
(defn planck-quanta->zeptos [n] (* n 269553/5000000000000000000000000000))
(defn sec->attos [n] (* n 1000000000000000000N))
(defn sec->centuries [n] (* n 1/3155760000))
(defn sec->common-years [n] (* n 1/31536000))
(defn sec->days [n] (* n 1/86400))
(defn sec->decades [n] (* n 1/315576000))
(defn sec->femtos [n] (* n 1000000000000000N))
(defn sec->fortnights [n] (* n 1/1209600))
(defn sec->hours [n] (* n 1/3600))
(defn sec->hrs [n] (* n 1/3600))
(defn sec->leap-years [n] (* n 1/31622400))
(defn sec->micros [n] (* n 1000000N))
(defn sec->millennia [n] (* n 1/31557600000))
(defn sec->millis [n] (* n 1000N))
(defn sec->min [n] (* n 1/60))
(defn sec->minutes [n] (* n 1/60))
(defn sec->months [n] (* n 1/2629800))
(defn sec->mos [n] (* n 1/2629800))
(defn sec->nanos [n] (* n 1000000000N))
(defn sec->picos [n] (* n 1000000000000N))
(defn sec->planck-quanta [n] (* n 5000000000000000000000000000000000000000000000000/269553))
(defn sec->weeks [n] (* n 1/604800))
(defn sec->wks [n] (* n 1/604800))
(defn sec->years [n] (* n 1/31557600))
(defn sec->yoctos [n] (* n 1000000000000000000000000N))
(defn sec->yrs [n] (* n 1/31557600))
(defn sec->zeptos [n] (* n 1000000000000000000000N))
(defn weeks->attos [n] (* n 604800000000000000000000N))
(defn weeks->centuries [n] (* n 7/36525))
(defn weeks->common-years [n] (* n 7/365))
(defn weeks->days [n] (* n 7N))
(defn weeks->decades [n] (* n 14/7305))
(defn weeks->femtos [n] (* n 604800000000000000000N))
(defn weeks->fortnights [n] (* n 1/2))
(defn weeks->hours [n] (* n 168N))
(defn weeks->hrs [n] (* n 168N))
(defn weeks->leap-years [n] (* n 7/366))
(defn weeks->micros [n] (* n 604800000000N))
(defn weeks->millennia [n] (* n 7/365250))
(defn weeks->millis [n] (* n 604800000N))
(defn weeks->min [n] (* n 10080N))
(defn weeks->minutes [n] (* n 10080N))
(defn weeks->months [n] (* n 112/487))
(defn weeks->mos [n] (* n 112/487))
(defn weeks->nanos [n] (* n 604800000000000N))
(defn weeks->picos [n] (* n 604800000000000000N))
(defn weeks->planck-quanta [n] (* n 1008000000000000000000000000000000000000000000000000000/89851))
(defn weeks->sec [n] (* n 604800N))
(defn weeks->wks [n] n)
(defn weeks->years [n] (* n 28/1461))
(defn weeks->yoctos [n] (* n 604800000000000000000000000000N))
(defn weeks->yrs [n] (* n 28/1461))
(defn weeks->zeptos [n] (* n 604800000000000000000000000N))
(defn wks->attos [n] (* n 604800000000000000000000N))
(defn wks->centuries [n] (* n 7/36525))
(defn wks->common-years [n] (* n 7/365))
(defn wks->days [n] (* n 7N))
(defn wks->decades [n] (* n 14/7305))
(defn wks->femtos [n] (* n 604800000000000000000N))
(defn wks->fortnights [n] (* n 1/2))
(defn wks->hours [n] (* n 168N))
(defn wks->hrs [n] (* n 168N))
(defn wks->leap-years [n] (* n 7/366))
(defn wks->micros [n] (* n 604800000000N))
(defn wks->millennia [n] (* n 7/365250))
(defn wks->millis [n] (* n 604800000N))
(defn wks->min [n] (* n 10080N))
(defn wks->minutes [n] (* n 10080N))
(defn wks->months [n] (* n 112/487))
(defn wks->mos [n] (* n 112/487))
(defn wks->nanos [n] (* n 604800000000000N))
(defn wks->picos [n] (* n 604800000000000000N))
(defn wks->planck-quanta [n] (* n 1008000000000000000000000000000000000000000000000000000/89851))
(defn wks->sec [n] (* n 604800N))
(defn wks->weeks [n] n)
(defn wks->years [n] (* n 28/1461))
(defn wks->yoctos [n] (* n 604800000000000000000000000000N))
(defn wks->yrs [n] (* n 28/1461))
(defn wks->zeptos [n] (* n 604800000000000000000000000N))
(defn years->attos [n] (* n 31557600000000000000000000N))
(defn years->centuries [n] (* n 1/100))
(defn years->common-years [n] (* n 1461/1460))
(defn years->days [n] (* n 1461/4))
(defn years->decades [n] (* n 1/10))
(defn years->femtos [n] (* n 31557600000000000000000N))
(defn years->fortnights [n] (* n 1461/56))
(defn years->hours [n] (* n 8766N))
(defn years->hrs [n] (* n 8766N))
(defn years->leap-years [n] (* n 487/488))
(defn years->micros [n] (* n 31557600000000N))
(defn years->millennia [n] (* n 1/1000))
(defn years->millis [n] (* n 31557600000N))
(defn years->min [n] (* n 525960N))
(defn years->minutes [n] (* n 525960N))
(defn years->months [n] (* n 12N))
(defn years->mos [n] (* n 12N))
(defn years->nanos [n] (* n 31557600000000000N))
(defn years->picos [n] (* n 31557600000000000000N))
(defn years->planck-quanta [n] (* n 52596000000000000000000000000000000000000000000000000000/89851))
(defn years->sec [n] (* n 31557600N))
(defn years->weeks [n] (* n 1461/28))
(defn years->wks [n] (* n 1461/28))
(defn years->yoctos [n] (* n 31557600000000000000000000000000N))
(defn years->yrs [n] n)
(defn years->zeptos [n] (* n 31557600000000000000000000000N))
(defn yoctos->attos [n] (* n 1/1000000))
(defn yoctos->centuries [n] (* n 1/3155760000000000000000000000000000))
(defn yoctos->common-years [n] (* n 1/31536000000000000000000000000000))
(defn yoctos->days [n] (* n 1/86400000000000000000000000000))
(defn yoctos->decades [n] (* n 1/315576000000000000000000000000000))
(defn yoctos->femtos [n] (* n 1/1000000000))
(defn yoctos->fortnights [n] (* n 1/1209600000000000000000000000000))
(defn yoctos->hours [n] (* n 1/3600000000000000000000000000))
(defn yoctos->hrs [n] (* n 1/3600000000000000000000000000))
(defn yoctos->leap-years [n] (* n 1/31622400000000000000000000000000))
(defn yoctos->micros [n] (* n 1/1000000000000000000))
(defn yoctos->millennia [n] (* n 1/31557600000000000000000000000000000))
(defn yoctos->millis [n] (* n 1/1000000000000000000000))
(defn yoctos->min [n] (* n 1/60000000000000000000000000))
(defn yoctos->minutes [n] (* n 1/60000000000000000000000000))
(defn yoctos->months [n] (* n 1/2629800000000000000000000000000))
(defn yoctos->mos [n] (* n 1/2629800000000000000000000000000))
(defn yoctos->nanos [n] (* n 1/1000000000000000))
(defn yoctos->picos [n] (* n 1/1000000000000))
(defn yoctos->planck-quanta [n] (* n 5000000000000000000000000/269553))
(defn yoctos->sec [n] (* n 1/1000000000000000000000000))
(defn yoctos->weeks [n] (* n 1/604800000000000000000000000000))
(defn yoctos->wks [n] (* n 1/604800000000000000000000000000))
(defn yoctos->years [n] (* n 1/31557600000000000000000000000000))
(defn yoctos->yrs [n] (* n 1/31557600000000000000000000000000))
(defn yoctos->zeptos [n] (* n 1/1000))
(defn yrs->attos [n] (* n 31557600000000000000000000N))
(defn yrs->centuries [n] (* n 1/100))
(defn yrs->common-years [n] (* n 1461/1460))
(defn yrs->days [n] (* n 1461/4))
(defn yrs->decades [n] (* n 1/10))
(defn yrs->femtos [n] (* n 31557600000000000000000N))
(defn yrs->fortnights [n] (* n 1461/56))
(defn yrs->hours [n] (* n 8766N))
(defn yrs->hrs [n] (* n 8766N))
(defn yrs->leap-years [n] (* n 487/488))
(defn yrs->micros [n] (* n 31557600000000N))
(defn yrs->millennia [n] (* n 1/1000))
(defn yrs->millis [n] (* n 31557600000N))
(defn yrs->min [n] (* n 525960N))
(defn yrs->minutes [n] (* n 525960N))
(defn yrs->months [n] (* n 12N))
(defn yrs->mos [n] (* n 12N))
(defn yrs->nanos [n] (* n 31557600000000000N))
(defn yrs->picos [n] (* n 31557600000000000000N))
(defn yrs->planck-quanta [n] (* n 52596000000000000000000000000000000000000000000000000000/89851))
(defn yrs->sec [n] (* n 31557600N))
(defn yrs->weeks [n] (* n 1461/28))
(defn yrs->wks [n] (* n 1461/28))
(defn yrs->years [n] n)
(defn yrs->yoctos [n] (* n 31557600000000000000000000000000N))
(defn yrs->zeptos [n] (* n 31557600000000000000000000000N))
(defn zeptos->attos [n] (* n 1/1000))
(defn zeptos->centuries [n] (* n 1/3155760000000000000000000000000))
(defn zeptos->common-years [n] (* n 1/31536000000000000000000000000))
(defn zeptos->days [n] (* n 1/86400000000000000000000000))
(defn zeptos->decades [n] (* n 1/315576000000000000000000000000))
(defn zeptos->femtos [n] (* n 1/1000000))
(defn zeptos->fortnights [n] (* n 1/1209600000000000000000000000))
(defn zeptos->hours [n] (* n 1/3600000000000000000000000))
(defn zeptos->hrs [n] (* n 1/3600000000000000000000000))
(defn zeptos->leap-years [n] (* n 1/31622400000000000000000000000))
(defn zeptos->micros [n] (* n 1/1000000000000000))
(defn zeptos->millennia [n] (* n 1/31557600000000000000000000000000))
(defn zeptos->millis [n] (* n 1/1000000000000000000))
(defn zeptos->min [n] (* n 1/60000000000000000000000))
(defn zeptos->minutes [n] (* n 1/60000000000000000000000))
(defn zeptos->months [n] (* n 1/2629800000000000000000000000))
(defn zeptos->mos [n] (* n 1/2629800000000000000000000000))
(defn zeptos->nanos [n] (* n 1/1000000000000))
(defn zeptos->picos [n] (* n 1/1000000000))
(defn zeptos->planck-quanta [n] (* n 5000000000000000000000000000/269553))
(defn zeptos->sec [n] (* n 1/1000000000000000000000))
(defn zeptos->weeks [n] (* n 1/604800000000000000000000000))
(defn zeptos->wks [n] (* n 1/604800000000000000000000000))
(defn zeptos->years [n] (* n 1/31557600000000000000000000000))
(defn zeptos->yoctos [n] (* n 1000N))
(defn zeptos->yrs [n] (* n 1/31557600000000000000000000000))