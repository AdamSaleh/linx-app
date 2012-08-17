(ns linx.crypto
  (:import
   [javax.crypto Cipher SecretKey KeyGenerator]
   [javax.crypto.spec SecretKeySpec])
  (:require
   [clojure.tools.logging :as log]
   [linx.model :as model]
   [ring.util.codec :as codec]))

(declare get-key)

(def ^:private algorithm "DES")
(def ^:private secret-key (delay (get-key)))

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

(defn- put-key!
  [key]
  (model/upsert! :objects :id {:id "crypto.key" :value (encoded-key key)} )
  key)

(defn- get-key
  []
  (let [saved (:value (model/find-one :objects :id "crypto.key"))]
    (if (nil? saved)
      (put-key! (.generateKey (KeyGenerator/getInstance algorithm)))
      (SecretKeySpec. (b64->bytes saved) algorithm))))

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
