(ns bookmark.controller
  ;;
  ;; Functions connecting http routes to the underlying model and back
  ;; again.
  ;;
  (:require
   [bookmark.db :as model]
   [bookmark.views :as view]
   [bookmark.cookie :as cookie]
   [clojure.string :as string]
   [clojure.data.json :as json]
   [clojure.tools.logging :as log]
   [digest :as digest]
   [ring.util.response :as response]))

;;-----------------------------------------------------------------------------

(defn- write-json-objectid
  [oid out escape-unicode?]
  (.print out (str "\"" oid "\"")))

(extend org.bson.types.ObjectId clojure.data.json/Write-JSON
        {:write-json write-json-objectid})

(defn- as-json
  [data]
  (-> (response/response (json/json-str data))
      (response/content-type "application/json;charset=UTF-8")))

(defn- authentic?
  [request]
  (if-let [cookie (cookie/get request)]
    (model/authentic? (:email cookie) (:password cookie))
    false))

(defn- status-response
  [status]
  (-> (response/response "")
      (response/status status)))

(defn- parse-tags
  [tag-string]
  (map string/trim (string/split tag-string #"[,]")))

;;-----------------------------------------------------------------------------

(defn redirect
  [redirect-to]
  (response/redirect redirect-to))

(defn home-page
  [request]
  (view/home-page request))

(defn login-page
  [request redirect-to]
  (if (authentic? request)
    (redirect redirect-to)
    (view/login-page request)))

(defn account-edit-page
  [request]
  (view/account-page request (cookie/get request)))

(defn authorize
  [request email password]
  (if-let [user (model/authentic? email (digest/md5 password))]
    (cookie/set! (response/response "") request user)
    (status-response 403)))

(defn logout
  [request redirect-to]
  (-> (response/redirect redirect-to)
      (cookie/unset! request)))

(defn update-account
  [request email password]
  (try
    (let [user (model/user! (:email (cookie/get request)) email password)]
      (cookie! (response/response "") request user))
    (catch Throwable t
      (log/error " -" t)
      (status-response 400))))

(defn join
  [request email password]
  (if-let [user (model/join! email password)]
    (cookie/set! (response/response "") request user)
    (status-response 400)))

(defn search
  [request terms]
  (as-json (model/search (:email (cookie/get request)) terms)))

(defn add-bookmark
  ([request name addr tags]
     (if-let [b (model/bookmark! (:email (cookie/get request)) name addr (parse-tags tags))]
       (status-response 201)
       (status-response 400)))
  ([request name addr tags cuid]
     (let [user (cookie/parse cuid)]
       (if (model/authentic? (:email user) (digest/md5 (:password user)))
         (-> (add-bookmark request name addr tags)
             (response/header "Access-Control-Allow-Origin" "*"))
         (status-response 401)))))
