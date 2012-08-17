(ns linx.crypto
  (:import
   [javax.crypto Cipher SecretKey KeyGenerator]
   [javax.crypto.spec SecretKeySpec])
  (:require
   [clojure.tools.logging :as log]
   [ring.util.codec :as codec]))

;; TODO: We have two concerns, here. En/decrypting, and key management. Fix!

(declare make-or-find-key)

(def ^:private algorithm "DES")
(def ^:private secret-key (delay (make-or-find-key)))

(defn- key-file
  []
  (str (System/getProperty "data.dir" (System/getProperty "user.dir" ".")) "/crypto.key"))

(defn- bytes->b64
  [bytes]
  (String. (codec/base64-encode bytes)))

(defn- b64->bytes
  [string]
  (codec/base64-decode string))

(defn- encoded-key
  ([k]
     (bytes->b64 (.getEncoded k)))
  ([]
     (encoded-key @secret-key)))

(defn- read-key-file
  []
  (log/info " - reading:" (key-file))
  (slurp (key-file)))

(defn- save-key-file
  [k]
  (log/info " - writing new key file to:" (key-file))
  (let [secret (encoded-key k)]
    (spit (key-file) secret)
    secret))

(defn- make-or-find-key
  []
  (try
    (SecretKeySpec. (b64->bytes (read-key-file)) algorithm)
    (catch Throwable t
      (save-key-file (.generateKey (KeyGenerator/getInstance algorithm))))))

(defn encrypt
  [string]
  (let [cipher (Cipher/getInstance algorithm)]
    (.init cipher Cipher/ENCRYPT_MODE @secret-key)
    (bytes->b64 (.doFinal cipher (.getBytes string)))))

(defn decrypt
  [b64]
  (let [cipher (Cipher/getInstance algorithm)]
    (.init cipher Cipher/DECRYPT_MODE @secret-key)
    (String. (.doFinal cipher (b64->bytes b64)))))
