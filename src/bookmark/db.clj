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
    (mc/ensure-index :users {:email 1} {:unique true})
    (mc/ensure-index :users {:password 1})
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

(defn- re-quote
  [s]
  (format "^\\Q%s\\E$" (string/trim s)))

(defn- scrub
  [m]
  (dissoc m :_id))

;;-----------------------------------------------------------------------------
;; Public
;;-----------------------------------------------------------------------------

;;-----------------------------------------------------------------------------
;; TODO: Verify true(-ish) email addresses (at least in format).
;;-----------------------------------------------------------------------------


;;-----------------------------------------------------------------------------

(defn users
  "Return a list of users."
  []
  (with-conn
    (map scrub (mc/find-maps "users"))))

(defn user
  "Return a user matching the email address (or the password, if provided)."
  ([email]
     (with-conn
       (scrub (first (mc/find-maps "users" {:email {$regex (re-quote email) $options "i"}})))))
  ([email password]
    (with-conn
      (scrub (first (mc/find-maps "users" {:email {$regex (re-quote email) $options "i"}
                                           :password password}))))))

(defn exists?
  "Return true if the user using the email address exists."
  [email]
  (not (nil? (user email))))

(defn authentic?
  "Return the user if authentic, or nil."
  [email password]
  (user email (digest/md5 password)))

(defn join
  [email password]
  (let [e (string/lower-case email)
        p (digest/md5 password)]
    (when (authentic? email password)
      (with-conn
        (mc/insert :users {:email e :password p})))))

;;-----------------------------------------------------------------------------
;;-----------------------------------------------------------------------------

(defn search
  [terms-string]
  (let [terms (str->words terms-string)
        exprs (map #(into {} {$regex % $options "i"}) terms)
        clauses (map #(into {} {$or [{:desc %}
                                     {:url %}
                                     {:tags {$elemMatch %}}]}) exprs)]
    (map scrub
         (with-conn
           (mql/with-collection "bookmarks"
             (mql/find {$or clauses})
             (mql/sort {:desc 1}))))))
