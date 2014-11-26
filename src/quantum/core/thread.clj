(ns quantum.core.thread (:gen-class))
(require
  '[quantum.core.ns          :as ns    :refer [defalias alias-ns]])
(ns/require-all *ns* :clj)
(require
  '[quantum.core.numeric     :as num]
  '[quantum.core.function    :as fn   :refer :all]
  '[quantum.core.logic       :as log  :refer :all]
  '[quantum.core.data.vector :as vec  :refer [catvec]]
  '[quantum.core.collections :as coll :refer :all]
  '[quantum.core.error       :as err  :refer [throw+ try+]]
  '[clojure.core.async :as async :refer [go <! >! alts!]])


;(def #^{:macro true} go      #'async/go) ; defalias fails with macros (does it though?)...
(def #^{:macro true} go-loop #'async/go-loop)
(defalias close!     async/close!)
;(defalias <!         async/<!)
(defalias <!!        async/<!!)
;(defalias >!         async/>!)
(defalias >!!        async/>!!)
(defalias chan       async/chan)
;(defalias alts!      async/alts!)
(defalias alts!!     async/alts!!)
(def #^{:macro true} thread #'async/thread)

(def  reg-threads (atom {})) ; {:thread1 :open :thread2 :closed :thread3 :close-req}
(defn stop-thread! [thread-id]
  (case (get @reg-threads thread-id)
    nil
    (println (str "Thread '" (name thread-id) "' is not registered."))
    :open
    (do (swap! reg-threads assoc thread-id :close-req)
        (println (str "Closing thread '" (name thread-id) "'..."))
        (while (not= :closed (get @reg-threads thread-id)))
        (println (str "Thread '" (name thread-id) "' closed.")))
    :closed
    (println (str "Thread '" (name thread-id) "' is already closed."))
    nil))
(defmacro thread- [& exprs] ; seems to be a little slow?
  `(let [ns-0# *ns*]
    (javax.swing.SwingUtilities/invokeLater
       (proxy [Runnable] []
         (run [] (binding [*ns* ns-0#] (do ~exprs)))))))

(def ^{:dynamic true} *thread-num* (.. Runtime getRuntime availableProcessors))
; Why you want to manage your threads when doing network-related things:
; http://eng.climate.com/2014/02/25/claypoole-threadpool-tools-for-clojure/
(defmacro thread+
  "Execute exprs in another thread, and return thread"
  ^{:attribution "thebusby.bagotricks" :contributors "Alex Gunnarson"}
  [thread-id & exprs]
  (if true ;(contains? @reg-threads thread-id)
      ;(throw+ {:type :key-exists
      ;         :message (str "Thread id '" (name thread-id) "' already exists.")})
      (do (swap! reg-threads assoc thread-id :open) ; it never closes, then
          `(-> (let [ns-0# *ns*]
                (doto (java.lang.Thread.
                         (fn [] (binding [*ns* ns-0#] (do ~exprs))))
                      (.start)))))))

(defn promise-concur [method max-threads func list-0]
  (let [count- (count list-0)
        chunk-size
          (if (= max-threads :max)
              count-
              (-> count- (/ max-threads) (num/round :type :up)))] ; round up from decimal chunks
    (loop [list-n list-0
           promises ()]
      (if (empty? list-n)
        promises
        (recur
          (drop chunk-size list-n)
          (conj promises
            (future ; one thread for each chunk
              (if (= method :for)
                  (doall (for [elem (take chunk-size list-n)]
                    (func elem)))
                  (doseq [elem (take chunk-size list-n)]
                    (func elem))))))))))
(defn concur [method max-threads func list-0]
  (map deref (promise-concur method max-threads func list-0)))

(defn promise-concur-go [method max-threads func list-0]
  (let [count- (count list-0)
        chunk-size
          (if (= max-threads :max)
              count-
              (-> count- (/ max-threads) (num/round :type :up)))] ; round up from decimal chunks
    (loop [list-n list-0
           promises []]
      (if (empty? list-n)
        promises
        (recur
          (drop chunk-size list-n)
          (conj promises ; [[ct0 (chan0)] [ct1 (chan1)] [ct2 (chan2)]]
            (let [chan-0  (chan)
          chunk-n (take chunk-size list-n)
                  chunk-size-n (count chunk-n)]
              (go ; one go block / "lightweight thread pool" for each chunk
                (doseq [elem chunk-n]
                  ;(println "(func elem):" (func elem))
                  (>! chan-0 (func elem)))) ; the thread blocks it anyway
              [chunk-size-n chan-0])))))))
(defn concur-go [method max-threads func list-0]
  (let [chans (promise-concur-go method max-threads func list-0)]
    (if (= method :for)
        (->> chans
             (map+ (compr #(doall
                             (for [n (range (first %))]
                               (<!! (second %))))
                          vec+))
             fold+
             (apply catvec))
        (doseq [chan-n chans] chan-n))))

(defn- thread-or
  "Call each of the fs on a separate thread. Return logical
  disjunction of the results. Short-circuit (and cancel the calls to
  remaining fs) on first truthy value returned."
  ^{:attribution "Michal Marczyk - https://gist.github.com/michalmarczyk/5992795"}
  [& fs]
  (let [ret (promise)
        fps (promise)]
    (deliver fps
             (doall (for [f fs]
                      (let [p (promise)]
                        [(future
                           (let [v (f)]
                             (locking fps
                               (deliver p true)
                               (if v
                                 (deliver ret v)
                                 (when (every? realized? (map peek @fps))
                                   (deliver ret nil))))))
                         p]))))
    (let [result @ret]
      (doseq [[fut] @fps]
        (future-cancel fut))
      result)))
 
(comment
 
  ;; prints :foo, but not :bar
  (thread-or #(do (Thread/sleep 1000) (println :foo) true)
             #(do (Thread/sleep 3000) (println :bar)))
  ;;= true
 
  ;; prints :foo and :bar
  (thread-or #(do (Thread/sleep 1000) (println :foo))
             #(do (Thread/sleep 3000) (println :bar)))
  ;;= nil
 
  )
(defn- thread-and
  "Computes logical conjunction of return values of fs, each of which
  is called in a future. Short-circuits (cancelling the remaining
  futures) on first falsey value."
  ^{:attribution "Michal Marczyk - https://gist.github.com/michalmarczyk/5991353"}
  [& fs]
  (let [done (promise)
        ret  (atom true)
        fps  (promise)]
    (deliver fps (doall (for [f fs]
                          (let [p (promise)]
                            [(future
                               (if-not (swap! ret #(and %1 %2) (f))
                                 (deliver done true))
                               (locking fps
                                 (deliver p true)
                                 (when (every? realized? (map peek @fps))
                                   (deliver done true))))
                             p]))))
    @done
    (doseq [[fut] @fps]
      (future-cancel fut))
    @ret))
 
(comment
 
  (thread-and (constantly true) (constantly true))
  ;;= true
 
  (thread-and (constantly true) (constantly false))
  ;;= false
 
  (every? false?
          (repeatedly 100000
                      #(thread-and (constantly true) (constantly false))))
  ;;= true
 
  ;; prints :foo, but not :bar
  (thread-and #(do (Thread/sleep 1000) (println :foo))
              #(do (Thread/sleep 3000) (println :bar)))
 
  )


