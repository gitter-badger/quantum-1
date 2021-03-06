(ns quantum.db.datomic.schema
  "Schema syncing and schema alteration functions.
   Like the forthcoming `posh.lib.schemas`."
  (:require [datascript.core          :as ds]
    #?(:clj [datomic.api              :as dat])
            [quantum.core.fn
              :refer [fn->]]
            [quantum.core.collections :as coll
              :refer [dissoc-in merge-deep]]
            [quantum.core.data.set    :as set]
            [quantum.db.datomic.core  :as dbc]
            [quantum.core.validate    :as v
              :refer [validate]]
            [quantum.core.data.validated :as dv]))

(defn ensure-schema-changes-valid
  "For DataScript db.
   Will not change any user data as a result of schema alteration.
   Assumes EAVT-indexed datoms."
  {:attribution "alexandergunnarson"
   :see-also    #{"https://github.com/tonsky/datascript/issues/174"}
   :performance "At least O(d•s), where d = # datoms and s = # schemas to be changed"
   :todo {0 "[:index true] -> [:index false] needs to take effect"
          1 ":many -> :one needs to be validated but allowed"
          2 "non-unique -> unique needs to be validated but allowed"
          3 "use proper logging, not `println`"
          4 "handle db/isComponent changes (necessary?)"}}
  [schemas schemas' datoms]
  (let [deleted       (set/difference (-> schemas keys set) (-> schemas' keys set))
        changed+added (->> schemas' (remove (fn [[k v]] (= (get schemas k) v))))
        illegal-schema-change
          (fn [k v v'] (throw (ex-info "Illegal schema change attempted" {:k k :v v :v' v'})))
        unclear-validation
          (fn [k v v'] (println "WARN:" "Unclear whether DataScript will validate" k v "->" v'))] ; TODO 3
    ; Ensure that no datoms are affected by deleted schemas
    (doseq [[schema-name schema] deleted]
      (doseq [[_ a _ _] datoms]
        (assert (not= a schema-name))))
    ; Ensure changed and added schemas are valid
    (doseq [[schema-name' schema'] changed+added]
      (let [schema (get schemas schema-name')]
        (doseq [[k v'] schema']
          (let [v (get schema k)]
            (case k
              :db/cardinality
              (when (= [v v'] [:db.cardinality/many :db.cardinality/one])
                (illegal-schema-change k v v')) ; TODO 1
              :db/index
              (when (and (true? v) (not v'))
                (println "WARN:" "Unindexing" k "won't take effect in previous datoms")) ; TODO 0, 3
              :db/valueType
              (cond (and schema (not= v v'))
                    (illegal-schema-change k v v')
                    (not schema)
                    (unclear-validation k v v'))
              :db/unique
              (cond
                (and (not v) v)
                (illegal-schema-change k v v') ; TODO 2
                (= [v v'] [:db.unique/value :db.unique/identity])
                (unclear-validation k v v')
                (= [v v'] [:db.unique/identity :db.unique/value])
                (unclear-validation k v v'))
              :db/isComponent
              (throw (ex-info "TODO" {})) ; TODO 4
              ; Doesn't validate other schema changes
              )))))))

(defn update-schemas
  "Updates the schemas of a DataScript db."
  {:attribution "alexandergunnarson"
   :see-also #{"metasoarous/datsync.client"
               "http://docs.datomic.com/schema.html#Schema-Alteration"
               "https://github.com/tonsky/datascript/issues/174"}}
  ([db f]
    (assert (dbc/mdb? db))
    (let [schemas  (:schema db)
          schemas' (f schemas)
          datoms   (ds/datoms db :eavt)]
      (ensure-schema-changes-valid schemas schemas' datoms)
      (-> (ds/init-db datoms schemas')
          (ds/db-with
            [{:db/ident  :type  }
             {:db/ident  :schema}])
          (ds/db-with
            (for [schema (keys schemas')]
              {:db/ident schema
               :type     [:db/ident :schema]}))))))

(defn update-schemas! [conn f]
  (assert (ds/conn? conn))
  (swap! conn update-schemas f))

(defn merge-schemas
  "Immutably merges schemas and/or schema attributes (`schemas`) into the database `db`."
  {:usage `(merge-schemas {:task:estimated-duration {:db/valueType :db.type/long}})}
  [db schemas]
    (if #?@(:clj [(dbc/db? db)
                  (for [[schema kvs] schemas]
                    (merge
                      {:db/id               schema
                       :db.alter/_attribute :db.part/db}
                      kvs))]
            :cljs [false false])
        (update-schemas db #(merge-deep % schemas))))

(defn merge-schemas!
  "Mutably merges (transacts) schemas and/or schema attributes (`schemas`) into the `conn`."
  [conn schemas]
    (if #?@(:clj  [(dbc/conn? conn)
                   @(dat/transact conn (merge-schemas schemas))] ; TODO may want to sync schema?
            :cljs [false false])
        (swap! conn merge-schemas schemas)))

(defn replace-schemas!
  "Mutably replaces schemas of the provided DataScript `conn`."
  [conn schemas]
    (assert (dbc/mconn? conn))
    (swap! conn update-schemas (constantly schemas)))

(defn dissoc-schema!
  "Mutably dissociates a schema from `conn`."
  [conn s k v]
    (if #?@(:clj  [(dbc/conn? conn)
                   @(dat/transact conn ; TODO may want to sync schema?
                      [[:db/retract s k v]
                       [:db/add :db.part/db :db.alter/attribute k]])]
            :cljs [false false]))
        (update-schemas! conn #(dissoc-in % [s k])))

#?(:clj
(defn transact-schemas!
  "Clojure-only, because schemas can only be added upon creation of the DataScript
   connection; they cannot be transacted.

   Schema changes to Datomic happen asynchronously.
   This waits until the schemas are available."
  {:todo #{"Make waiting more robust"}}
  ([] (transact-schemas! nil))
  ([{:keys [conn schemas]}]
    (let [conn       (or conn @dbc/conn*)
          _          (validate conn dbc/conn?)
          schemas-tx (->> (or schemas (->> @dv/spec-infos (map (fn-> val :schema))))
                          (mapv #(assoc % :db/id (dbc/tempid (dbc/->db conn) :db.part/db))))
          tx-report  @(dat/transact conn schemas-tx)
          tx-id      (-> tx-report :tx-data ^datomic.Datom first (.tx))
          _ #_(deref (d/sync (->conn conn) (java.util.Date. (System/currentTimeMillis))) 500 nil)
              (deref (dat/sync-schema conn (inc tx-id)) 500 nil)] ; frustratingly, doesn't even work with un-`inc`ed txn-id
      tx-report))))
