(ns bookmark.migration
  (:import
   [java.util.logging LogManager Handler])
  (:require
   [clojure.tools.logging :as log]
   [monger.core :as mg]
   [monger.collection :as mc]
   [digest :as digest]))

(defn- quiet-java-logging!
  []
  (let [root (.getLogger (LogManager/getLogManager) "")
        handler (first (.getHandlers root))]
    (.removeHandler root handler)))

(quiet-java-logging!)

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

(defmacro with-conn [& body]
  `(try
     (do
       (init-mongo)
       ~@body)
     (catch Throwable t#
       (reset! conn nil)
       (log/error t#)
       (throw t#))))

;; Seed data

(def ^:private data
  [{:desc "Google Search",
    :url "http://google.com",
    :tags ["search" "google"]}
   {:desc "Clojure Main Site",
    :url "http://clojure.org",
    :tags ["clojure"]}
   {:desc "Mountain Lion OSX",
    :url "http://www.apple.com/osx/",
    :tags ["osx" "apple" "mac"]}
   {:desc "JDK 6 API",
    :url "http://docs.oracle.com/javase/6/docs/api/",
    :tags ["java" "jdk6"]}
   {:desc "JDK 7 API",
    :url "http://docs.oracle.com/javase/7/docs/api/",
    :tags ["java" "jdk7"]}
   {:desc "Clojure Mailing List",
    :url "https://groups.google.com/forum/?fromgroups#!forum/clojure",
    :tags ["clojure" "google" "forum"]}
   {:desc "Tumblr Dashboard"
    :url "http://www.tumblr.com/dashboard"
    :tags ["blog" "tumblr"]}
   {:desc "Monger"
    :url "http://clojuremongodb.info/"
    :tags ["clojure" "mongodb" "db"]}
   {:desc "Mongoika"
    :url "https://github.com/yuushimizu/Mongoika"
    :tags ["clojure" "mongodb" "github"]}])

(defn- finish-bookmark
  [b]
  (assoc b
    :owner "keith@zentrope.com"
    :timestamp (System/currentTimeMillis)
    :id (digest/sha1 (:url b))))

(defn- default-set
  []
  (map finish-bookmark data))

(defn- init
  []
  (with-conn
    (doseq [b (default-set)]
      (when (not (mc/find-one :bookmarks {:id (:id b)}))
        (mc/remove :bookmarks {:id (:id b)})
        (mc/insert :bookmarks b)))))

(defn -main
  [& args]
  (log/info "Attempting to initialize mongo.")
  (init)
  (log/info "Done."))
