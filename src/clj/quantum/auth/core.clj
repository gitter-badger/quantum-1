(ns
  ^{:doc "Auxiliary functions for authorization and key retrieval.
          Mainly interfaces with persistent storage where keys are
          stored."
    :attribution "Alex Gunnarson"}
  quantum.auth.core
  (:require-quantum [:lib]))

; TODO: /assoc/ for file; /update/ for file; overarching syntax
; https://developer.mozilla.org/docs/Security/Weak_Signature_Algorithm
; "This site makes use of a SHA-1 Certificate; it's recommended you use certificates with signature algorithms that use hash functions stronger than SHA-1."

(def auth-source-table
  {:google   "Google"
   :facebook "Facebook"
   :fb       "Facebook"
   :snapchat "Snapchat"
   :amazon   "Amazon"
   :intuit   "Intuit"
   :twitter  "Twitter"
   :quip     "Quip"})

(defn auth-keys
  "Retrieves authorization keys associated with the given authorization source @auth-source (e.g. Google, Facebook, etc.)."
  [^Keyword auth-source]
  (io/read
    :path [:resources "Keys"
           (str (get auth-source-table auth-source) ".cljx")]))

(defn datum
  {:usage '(datum :google :password)
   :out   "__my-password__"}
  [auth-source & ks]
  (get-in (auth-keys auth-source) ks))

(defn write-auth-keys!
  "Writes the given authorization keys to a file."
  [^Key auth-source map-f]
  (io/write!
    :path      [:resources "Keys"
                (str (get auth-source-table auth-source) ".cljx")]
    :overwrite false
    :data      map-f))

(defn keys-assoc! [^Key auth-source & ks]
  (write-auth-keys! auth-source
    (apply assoc-in (auth-keys auth-source) ks)))

(defn keys-dissoc! [^Key auth-source & ks]
  (write-auth-keys! auth-source
    (apply dissoc-in+ (auth-keys auth-source) ks)))

(defn access-token
  "Retrieves the current access token for @auth-source."
  [^Key auth-source service]
  (-> (auth-keys auth-source) (get service) :access-tokens :current :access-token))


