(ns
  ^{:doc "Simple logging capabilities. Keeps a global log, has a status channel,
          prints only if the level is enabled, etc.

          By no means a full-fledged logging system, but useful nonetheless."
    :attribution "Alex Gunnarson"}
  quantum.core.log
  (:refer-clojure :exclude [pr seqable?])
  (:require
    [com.stuartsierra.component   :as component]
    [quantum.core.core            :as qcore
      :refer [seqable?]]
    [quantum.core.macros.core     :as cmacros
      :refer [#?(:clj compile-if)]]
    [quantum.core.fn              :as fn       ]
    [quantum.core.meta.debug      :as debug    ]
    [quantum.core.print           :as pr       ])
#?(:cljs
  (:require-macros
    [quantum.core.log :as self])))

#?(:cljs (enable-console-print!))

(defonce outs
  (atom #?(:clj  (if-let [out-path (System/getProperty "quantum.core.log:out-file")]
                     (let [_   (binding [*out* *err*] (println "Logging to" out-path))
                           fos (-> out-path
                                   (java.io.FileOutputStream.  )
                                   (java.io.OutputStreamWriter.)
                                   (java.io.BufferedWriter.    ))]
                       (fn [] [*err* fos]))
                     (fn [] [*err*])) ; in order to not print to file
           :cljs (fn [] [*out*]))))

; TODO maybe use Timbre?

(defrecord
  ^{:doc "This is a record and not a map because it's quicker
          to check the default levels (member access: O(1)) than
          it would be with a hash-map (O(log32(n)))."}
  LoggingLevels
  [warn user alert info inspect debug macro-expand trace env])

(defonce levels
  (atom (map->LoggingLevels
          {:warn true
           :user true
           :ns   true})))

(defonce log (atom []))

(defrecord LogEntry
  [time-stamp ; ^DateTime
   type       ; ^Keyword
   ns-source  ; ^Namespace
   message])  ; ^String

(defn disable!
  {:in-types '{pr-type keyword?}}
  ([pr-type]
    (swap! levels assoc pr-type false))
  ([pr-type & pr-types]
    (doseq [pr-type-n (conj pr-types pr-type)]
      (disable! pr-type-n))))

(defn enable!
  {:in-types '{pr-type keyword?}}
  ([pr-type]
    (swap! levels assoc pr-type true))
  ([pr-type & pr-types]
    (doseq [pr-type-n (conj pr-types pr-type)]
      (enable! pr-type-n))))

(defrecord LogInitializer
  [levels]
  component/Lifecycle
  (start [this]
    (apply enable! (or levels #{:debug :warn}))
    this)
  (stop  [this]
    #_(apply disable! levels) ; we don't necessarily want this logic (?)
    this))

(defn ->log-initializer [{:keys [levels] :as opts}]
  (when-not (seqable? levels)
    (throw (new #?(:clj Exception :cljs js/Error) "@levels is not seqable")))

  (LogInitializer. levels))

(swap! qcore/registered-components assoc ::log ->log-initializer)

(defn pr*
  "Prints to |System/out| if the print alert type @pr-type
   is in the set of enabled print alert types, |levels|.

   Logs the printed result to the global log |log|."
  {:attribution "Alex Gunnarson"}
  [trace? pretty? print-fn pr-type args opts]
    (when (or (get @levels pr-type)
              #_(:cljs (= pr-type :macro-expand)))
      (let [trace?  (or (:trace?  opts) trace? )
            pretty? (or (:pretty? opts) pretty?)
            stack   (or (:stack   opts) -1     )
            timestamp? (:timestamp? opts)
            curr-fn (when trace? (debug/this-fn-name stack))
            args-f  (when args @args)
            env-type-str
              (when (get @levels :env)
                (str (name qcore/lang) " »"))
            out-str
              (with-out-str
                (when (= pr-type :macro-expand) (print "\n/* "))
                #?(:clj
                  (when timestamp?
                    (let [timestamp
                           (compile-if (do (Class/forName "java.time.format.DateTimeFormatter")
                                           (Class/forName "java.time.LocalDateTime"))
                             (.format
                               (java.time.format.DateTimeFormatter/ofPattern
                                 "MM-dd-yyyy HH:mm::ss")
                               (java.time.LocalDateTime/now))
                             nil)] ; TODO JDK < 8 timestamp
                      (print (str "[" timestamp "] ")))))
                (when trace?
                  (print "[")
                  (print #?(:clj (.getName (Thread/currentThread)))
                         ":"
                         curr-fn "»"
                         (str pr-type "] ")))
                (if (and pretty? (-> args-f first string?))
                    (do (print (first args-f) " ")
                        (println)
                        (apply print-fn (rest args-f)))
                    (do (when pretty? (println))
                        (apply print-fn args-f)))
                (when (= pr-type :macro-expand) (print " */\n")))]

#?(:clj  (doseq [out (@outs)] (binding [*out* out] (print out-str) (flush)))
   :cljs (let [console-print-fn
                (or (aget js/console (name pr-type)) println)]
           (console-print-fn out-str)))
        (when (:log? opts)
          (swap! quantum.core.log/log conj
            (LogEntry.
              "TIMESTAMP" #_(time/now)
              pr-type
              curr-fn
              out-str)))))
    true) ; for :post logging

; TODO make these more efficient
#?(:clj
(defmacro pr [pr-type & args]
  `(pr* true  false println ~pr-type (delay (list ~@args)) nil)))

#?(:clj
(defmacro pr-no-trace [pr-type & args]
  `(pr* false false println ~pr-type (delay (list ~@args)) nil)))

#?(:clj
(defmacro pr-opts [pr-type opts & args]
  `(pr* true false println ~pr-type (delay (list ~@args)) ~opts)))

#?(:clj
(defmacro ppr [pr-type & args]
  `(pr* true  true  pr/!    ~pr-type (delay (list ~@args)) nil)))

#?(:clj
(defmacro ppr-hints [pr-type & args]
  `(pr* true true  pr/pprint-hints ~pr-type (delay (list ~@args)) nil)))

#?(:clj
(defmacro prl
  "'Print labeled'.
   Puts each x in `xs` as vals in a map.
   The keys in the map are the quoted vals. Then prints the map."
  [level & xs]
  `(let [level# ~level]
     (ppr level# ~(->> xs (map #(vector (list 'quote %) %)) (into {}))))))

#?(:clj
(defmacro this-ns []
  `(pr* true false println :ns (delay ['~(ns-name *ns*)]) nil)))
