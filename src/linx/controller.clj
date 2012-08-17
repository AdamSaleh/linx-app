(ns linx.controller
  ;;
  ;; Functions connecting http routes to the underlying model and back
  ;; again.
  ;;
  (:require
   [linx.model :as model]
   [linx.view :as view]
   [linx.cookie :as cookie]
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

(defn- slim
  [s]
  (string/trim (string/lower-case s)))

(defn- really=
  [^String s1 ^String s2]
  (= (slim s1)
     (slim s2)))

(defn- authorized-owner
  [request bookmark-id]
  (let [user (:email (cookie/get request))
        owner (:email (model/find-one :bookmarks :id bookmark-id))]
    (really= user owner)))

(defn- log-unauthorized-bookmark-access
  [request bookmark-id action]
  (let [c (cookie/get request)
        b (model/find-one :bookmarks :id bookmark-id)
        e {:action action :cookie c :bookmark b}]
    (log/error " - illegal bookmark access:" e)))

(defmacro respond
  [code & forms]
  `(do
     ~@forms
     (status-response ~code)))

(defn- update-bookmark!
  [request id url desc tags]
  (let [owner (:email (cookie/get request))
        bookmark {:email owner :desc desc :url url :tags tags}]
    (model/remove! :bookmarks :id id)
    (model/upsert! :bookmarks :id bookmark)))

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
    (let [old-email (slim (:email (cookie/get request)))
          new-email (slim email)
          user (model/user! old-email new-email password)]
      (model/update! :bookmarks {:email old-email} {:email new-email})
      (cookie/set! (response/response "") request user))
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
     (let [{:keys [email password]} (cookie/parse cuid)]
       (if (model/authentic? email password)
         (if (model/bookmark! email name addr (parse-tags tags))
           (-> (status-response 201)
               (response/header "Access-Control-Allow-Origin" "*"))
           (status-response 400))
         (status-response 401)))))

(defn delete-bookmark
  [request bookmark-id]
  (if (authorized-owner request bookmark-id)
    (respond 201 (model/remove! :bookmarks :id bookmark-id))
    (respond 400 (log-unauthorized-bookmark-access request bookmark-id :delete))))

(defn update-bookmark
  [request id url desc tags]
  (if (authorized-owner request id)
    (respond 201 (update-bookmark! request id url desc tags))
    (respond 400 (log-unauthorized-bookmark-access request id :update))))
