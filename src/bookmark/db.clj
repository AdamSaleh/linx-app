(ns bookmark.db
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
    (mc/ensure-index :bookmarks {:desc 1})
    (mc/ensure-index :bookmarks {:tags 1})
    (mc/ensure-index :bookmarks {:owner 1})
    (mc/ensure-index :bookmarks {:timestamp 1})
    (mc/ensure-index :bookmarks {:id 1} {:unique true})
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

;;-----------------------------------------------------------------------------

(defn- finish-bookmark
  [b]
  (assoc b
    :owner "keith@zentrope.com"
    :timestamp (System/currentTimeMillis)
    :id (digest/sha1 (:url b))))

(defn- save-bookmark
  [b]
  (let [new-b (finish-bookmark b)]
    (with-conn
      (mc/remove :bookmarks {:id (:id new-b)})
      (mc/insert :bookmarks new-b))))

(defn- str->words
  [string]
  (-> (string/lower-case string)
      (string/trim)
      (string/split #"\s+")
      (set)))

;;-----------------------------------------------------------------------------
;; Public
;;-----------------------------------------------------------------------------

(defn search
  [terms-string]
  (let [terms (str->words terms-string)
        exprs (map #(into {} {$regex % $options "i"}) terms)
        clauses (map #(into {} {$or [{:desc %}
                                     {:url %}
                                     {:tags {$elemMatch %}}]}) exprs)]
    (map #(dissoc % :_id)
         (with-conn
           (mql/with-collection "bookmarks"
             (mql/find {$or clauses})
             (mql/sort {:desc 1}))))))
