(ns bookmark.server
  (:gen-class)
  (:import
   [clojure.lang APersistentMap])
  (:use
   compojure.core)
  (:require
   [bookmark.db :as db]
   [bookmark.crypto :as crypto]
   [compojure.route :as route]
   [compojure.handler :as handler]
   [ring.adapter.jetty :as jetty]
   [ring.util.response :as resp]
   [clojure.string :as string]
   [clojure.data.json :as json]
   [clojure.tools.logging :as log]))

(def ^:private max-age-seconds (* 30 24 60 60)) ;; 30 days
(def ^:private cookie-name "book-app")

;;-----------------------------------------------------------------------------
;; Request helpers
;;-----------------------------------------------------------------------------

(defn- request->str
  [request]
  (let [{:keys [uri scheme request-method server-name server-port query-string]} request]
    (format "%s %s://%s:%s%s%s"
            (string/upper-case (name request-method))
            (name scheme)
            server-name
            server-port
            uri
            (if (empty? query-string) "" (str "?" query-string)))))

(defn- public-path?
  "Is the request allowed to proceed even if not authorized/authenticated?"
  [request]
  (let [u (:uri request)]
    (or (.startsWith u "/bm/login")
        (.startsWith u "/bm/css")
        (.startsWith u "/bm/js")
        (.startsWith u "/bm/html")
        (.startsWith u "/bm/api/auth"))))

(defn- unset-cookie!
  [response request]
  (resp/set-cookie response cookie-name ""
                   {:domain (:server-name request)
                    :port (:server-port request)
                    :path "/bm"
                    :max-age -1}))

(defn- cookie!
  "Adds the set-cookie command to the response."
  [response request cookie-value]
  (resp/set-cookie response cookie-name cookie-value {:domain (:server-name request)
                                                      :port (:server-port request)
                                                      :path "/bm"
                                                      :max-age max-age-seconds}))

(defn- cookie
  [request]
  "Returns an evalution of the decrypted cookie, if it exists."
  (if-let [cookie (get-in (:cookies request) [cookie-name :value])]
    (read-string (crypto/decrypt cookie))
    nil))

(defn- logged-in?
  "The request is logged in if a cookie is present."
  [request]
  (not (nil? (cookie request))))

(defn- authentic?
  "Is the user implied by the cookie authentic?"
  [request]
  (if-let [cookie (cookie request)]
    (db/exists? (:email cookie))
    false))

(defn- status-response
  [status]
  (-> (resp/response "")
      (resp/status status)))

;;-----------------------------------------------------------------------------
;; Middleware
;;-----------------------------------------------------------------------------

(defn- wrap-request-logger
  "Logs the request, and that's that."
  [handler]
  (fn [request]
    (log/info (request->str request))
    (handler request)))

(defn- wrap-auth
  "Redirects request to the login page if not authenticated/authorized."
  [handler]
  (fn [request]
    (log/info " - cookie:" (cookie request))
    (cond (authentic? request)
          (handler request)
          (public-path? request)
          (handler request)
          :else
          (do
            (log/info (format " - Denied access on %s, sending to /bm/login/." (:uri request)))
            (resp/redirect "/bm/login/")))))

;;-----------------------------------------------------------------------------
;; Renders
;;-----------------------------------------------------------------------------

(defn- as-json
  [^APersistentMap data]
  (-> (resp/response (json/json-str data))
      (resp/content-type "application/json;charset=UTF-8")))

;;-----------------------------------------------------------------------------
;; Routes
;;-----------------------------------------------------------------------------

(defroutes main-routes
  (GET "/bm" []
    (resp/redirect "/bm/"))

  (context "/bm" []

    (GET "/" []
      (resp/resource-response "html/index.html" {:root "public/bm"}))

    (GET "/login" []
      (resp/redirect "/bm/login/"))

    (GET "/login/" [:as request]
      (resp/resource-response "html/login.html" {:root "public/bm"}))

    (GET "/logout/" [:as request]
      (-> (resp/redirect "/bm/login/")
          (unset-cookie! request)))

    (context "/api" []
      ;;
      (POST "/auth/" [email password :as request]
        (log/info " - request to auth." {:email email :password password})
        (if-let [user (db/authentic? email password)]
          (do
            (log/info " - valid auth.")
            (-> (resp/response "")
                (cookie! request (crypto/encrypt (str user)))))
          (do
            (log/info " - invalid auth.")
            (status-response 403))))
      ;;
      (POST "/search/" [terms]
        (as-json (db/search terms)))))
  ;;
  (route/resources "/")
  (route/not-found "<a href='/bm/'>Page not found.</a>"))

(def ^:private app (-> main-routes
                       (wrap-auth)
                       (handler/site)
                       (wrap-request-logger)))

(def ^:private server (atom nil))


(defn- start
  ([opts]
     (log/info "Starting server.")
     (reset! server (jetty/run-jetty
                     (var app)
                     (merge {:port 8087 :join? false} opts))))
  ([]
     (start {})))

(defn- stop
  []
  (log/info "Stopping server.")
  (when (not (nil? @server))
    (.stop @server)
    (reset! server nil)))

(defn -main
  [& args]
  (log/info "Hello, World!")
  (start {:join? true}))
