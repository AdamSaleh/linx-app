(ns bookmark.cookie
  (:require
   [ring.util.response :as response]
   [bookmark.crypto :as crypto])
  (:refer-clojure :exclude [get]))

(def cookie-name "book-app")
(def cookie-path "/bm")
(def cookie-age (* 30 24 60 60)) ;; 30 days

(defn- mk-params
  [request age]
  {:domain (:server-name request)
   :port (:server-port request)
   :path cookie-path
   :max-age age})

(defn raw
  [request]
  (get-in (:cookies request) [cookie-name :value]))

(defn parse
  [cookie]
  (read-string (crypto/decrypt cookie)))

(defn unset!
  [resp req]
  (response/set-cookie resp cookie-name "" (mk-params req -1)))

(defn get
  [request]
  (if-let [cookie (raw request)]
    (parse cookie)
    nil))

(defn set!
  [resp request user]
  (let [value  (crypto/encrypt (str (select-keys user [:email :password])))
        params (mk-params request cookie-age)]
    (response/set-cookie resp cookie-name value params)))

(defn valid?
  [request]
  (try
    (let [{:keys [email password]} (get request)]
      (not (or (nil? email) (nil? password))))
    (catch Throwable t
      false)))
