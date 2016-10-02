(def ^:private ^long bits [] nil)
(def ^:private ^int length nil)
(def ^:private ^BitSieve smallSieve (BitSieve.))
(defnt
  ^:private
  ^:constructor
  BitSieve
  {:doc ["Construct a \"small sieve\" with a base of 0.  This constructor is"
         "used internally to generate the set of \"small primes\" whose multiples"
         "are excluded from sieves generated by the main (package private)"
         "constructor, BitSieve(BigInteger base, int searchLen).  The length"
         "of the sieve generated by this constructor was chosen for performance;"
         "it controls a tradeoff between how much time is spent constructing"
         "other sieves, and how much time is wasted testing composite candidates"
         "for primality.  The length was chosen experimentally to yield good"
         "performance."]}
  []
  (set! length (* 150 64))
  (set! bits (->multi-array nil [(inc* (unitIndex (dec* length)))]))
  (set 0)
  (let [^int nextIndex 1])
  (let [^int nextPrime 3])
  (loop
   []
    (sieveSingle length (+ nextIndex nextPrime) nextPrime)
    (set! nextIndex (sieveSearch length (inc* nextIndex)))
    (set! nextPrime (inc* (>> nextIndex 1)))
    (when (and (pos? nextIndex) (< nextPrime length)) (recur))))
(defnt
  ^:constructor
  BitSieve
  {:doc ["Construct a bit sieve of searchLen bits used for finding prime number"
         "candidates. The new sieve begins at the specified base, which must"
         "be even."]}
  [^BigInteger base ^int searchLen]
  (set!
   bits
   (->multi-array nil [(inc* (unitIndex (dec* searchLen)))]))
  (set! length searchLen)
  (let [^int start 0])
  (let
   [^int step (.sieveSearch smallSieve (.length smallSieve) start)])
  (let [^int convertedStep (inc* (>> step 1))])
  (let [^MutableBigInteger b (MutableBigInteger. base)])
  (let [^MutableBigInteger q (MutableBigInteger.)])
  (loop
   []
    (set! start (.divideOneWord b convertedStep q))
    (set! start (- convertedStep start))
    (when (zero? (rem start 2)) (swap! start + convertedStep))
    (sieveSingle searchLen (/ (dec* start) 2) convertedStep)
    (set!
     step
     (.sieveSearch smallSieve (.length smallSieve) (inc* step)))
    (set! convertedStep (inc* (>> step 1)))
    (when (pos? step) (recur))))
(defnt
  ^:private
  unitIndex
  {:doc ["Given a bit index return unit index containing it."]}
  [^int bitIndex]
  (>>> bitIndex 6))
(defnt
  ^:private
  bit
  {:doc ["Return a unit that masks the specified bit in its unit."]}
  [^int bitIndex]
  (<< (long 1) (& bitIndex (dec* (<< 1 6)))))
(defnt
  ^:private
  get
  {:doc ["Get the value of the bit at the specified index."]}
  [^int bitIndex]
  (let [^int unitIndex (unitIndex bitIndex)])
  (not= (& (aget bits unitIndex) (bit bitIndex)) 0))
(defnt
  ^:private
  set
  {:doc ["Set the bit at the specified index."]}
  [^int bitIndex]
  (let [^int unitIndex (unitIndex bitIndex)])
  (swap! (aget bits unitIndex) | (bit bitIndex)))
(defnt
  ^:private
  sieveSearch
  {:doc ["This method returns the index of the first clear bit in the search"
         "array that occurs at or after start. It will not search past the"
         "specified limit. It returns -1 if there is no such clear bit."]}
  [^int limit ^int start]
  (when (>= start limit) (return -1))
  (let [^int index start])
  (loop
   []
    (when (not (get index)) (return index))
    (inc! index)
    (when (< index (dec* limit)) (recur)))
  -1)
(defnt
  ^:private
  sieveSingle
  {:doc ["Sieve a single set of multiples out of the sieve. Begin to remove"
         "multiples of the specified step starting at the specified start index,"
         "up to the specified limit."]}
  [^int limit ^int start ^int step]
  (while (< start limit) (set start) (swap! start + step)))
(defnt
  retrieve
  {:doc ["Test probable primes in the sieve and return successful candidates."]}
  [^BigInteger initValue ^int certainty ^java.util.Random random]
  (let [^int offset 1])
  (ifor
   [(let [^int i 0])]
   (< i (.length bits))
   (inc! i)
   (let [^long nextLong (bit-not (aget bits i))])
   (ifor
    [(let [^int j 0])]
    (< j 64)
    (inc! j)
    (when
     (= (& nextLong 1) 1)
      (do
        (let
         [^BigInteger
          candidate
          (.add initValue (.valueOf BigInteger offset))])
        (when
         (.primeToCertainty candidate certainty random)
          (return candidate))))
    (swap! nextLong >>> 1)
    (swap! offset + 2)))
  nil)
