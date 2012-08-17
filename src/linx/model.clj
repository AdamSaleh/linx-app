(ns linx.model
  (:use
   monger.operators)
  (:require
   [digest :as digest]
   [clojure.string :as string]
   [clojure.tools.logging :as log]
   [monger.core :as mg]
   [monger.collection :as mc]
   [monger.query :as mql]))

;;-----------------------------------------------------------------------------

(def ^:private conn (atom nil))

(defn- init-mongo
  []
  (when (nil? @conn)
    (mg/connect!)
    (mg/set-db! (mg/get-db "bookmark-manager"))
    ;;
    (mc/ensure-index :users {:email 1} {:unique true})
    (mc/ensure-index :users {:password 1})
    ;;
    (mc/ensure-index :bookmarks {:desc 1})
    (mc/ensure-index :bookmarks {:tags 1})
    (mc/ensure-index :bookmarks {:email 1})
    (mc/ensure-index :bookmarks {:timestamp 1})
    (mc/ensure-index :bookmarks {:id 1} {:unique true})
    ;;
    (mc/ensure-index :objects {:id 1})
    ;;
    (reset! conn :working)))

(defmacro with-conn
  [& body]
  `(try
     (do
       (init-mongo)
       ~@body)
     (catch Throwable t#
       (reset! conn nil)
       (log/error t#)
       (throw t#))))

(defn- str->words
  [string]
  (-> (string/lower-case string)
      (string/trim)
      (string/split #"\s+")
      (set)))

(defn- re-quote
  [s]
  (format "^\\Q%s\\E$" (string/trim s)))

(defn- now
  []
  (System/currentTimeMillis))

;;-----------------------------------------------------------------------------
;; Public
;;-----------------------------------------------------------------------------

(def user-doc [:_id :email :password])
(def bookmark-doc [:id :id :email :desc :url :tags :timestamp])

;; Persistence API

(defmulti pre-process
  "Invoked before mutating a document in a given collection."
  (fn [collection document] collection))

(defmulti find-one
  "Returns a single document with pkey=value, or nil."
  (fn [collection pkey value] collection))

(defmulti find-*
  "Returns all the documents in a collection matching the key/value clauses."
  (fn [collection & clauses] collection))

(defmulti find-one*
  "Returns a single document from a collection matching the key/value clauses."
  (fn [collection & clauses] collection))

(defmulti update!
  "Updates values in all the matching documents in a collection."
  (fn [collection match-map values-map] collection))

(defmulti upsert!
  "Replaces a document in a collection."
  (fn [collection pkey document] collection))

(defmulti remove!
  "Removes a document where pkey=value from a collection."
  (fn [collection pkey value] collection))

;; Implementations

(defmethod pre-process
  :default
  [collection document]
  document)

(defmethod pre-process
  :bookmarks
  [collection bookmark]
  (select-keys (assoc bookmark
                 :timestamp (now)
                 :id (digest/sha1 (:url bookmark)))
               bookmark-doc))

(defmethod pre-process
  :users
  [collection document]
  (select-keys document user-doc))

(defmethod find-*
  :default
  [collection & clauses]
  (with-conn (mc/find-maps collection (apply hash-map clauses))))

(defmethod find-one*
  :default
  [collection & clauses]
  (with-conn (mc/find-one-as-map collection (apply hash-map clauses))))

(defmethod find-one
  :default
  [collection id value]
  (with-conn (mc/find-one-as-map collection {id {$regex (re-quote value) $options "i"}})))

(defmethod update!
  :default
  [collection match-map values-map]
  (with-conn (mc/update collection match-map {$set values-map} :multi true)))

(defmethod upsert!
  :default
  [collection pkey document]
  (let [entity (pre-process collection document)]
    (with-conn
      (mc/remove collection {pkey {$regex (re-quote (pkey entity)) $options "i"} })
      (mc/insert-and-return collection entity))))

(defmethod remove!
  :default
  [collection pkey value]
  (with-conn (mc/remove collection {pkey {$regex (re-quote value) $options "i"}})))

;; Convenience functions

(defn users
  []
  (find-* :users))

(defn user
  ([email]
     (find-one :users :email email))
  ([email password-hash]
     (find-one* :users :email email :password password-hash)))

(defn user!
  [old-email email password]
  (remove! :users :email old-email)
  (upsert! :users :email {:email (string/lower-case email) :password (digest/md5 password)}))

(defn authentic?
  [email password-hash]
  (find-one* :users :email email :password password-hash))

(defn bookmark!
  [email name addr tags]
  (upsert! :bookmarks :id {:email email :desc name :url addr :tags tags}))

(defn exists?
  [email]
  (not (nil? (user email))))

(defn join!
  [email raw-password]
  (if (not (exists? email))
    (user! email email raw-password)
    nil))

(defn search
  [email terms-string]
  (let [terms (str->words terms-string)
        exprs (map #(into {} {$regex % $options "i"}) terms)
        clauses (map #(into {} {$or [{:desc %} {:url %} {:tags {$elemMatch %}}]}) exprs)]
    (with-conn
      (mql/with-collection "bookmarks"
        (mql/find {$and [{:email {$regex (re-quote email) $options "i"}}
                         {$or clauses}]})
        (mql/sort {:desc 1})))))
