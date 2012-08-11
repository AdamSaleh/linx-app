(ns bookmark.server
  (:gen-class)
  (:require
   [bookmark.db :as db]
   [bookmark.cookie :as cookie]
   [bookmark.crypto :as crypto]
   [bookmark.views :as views]
   [bookmark.controller :as controller]
   [compojure.route :as route]
   [compojure.handler :as handler]
   [ring.adapter.jetty :as jetty]
   [ring.util.response :as resp]
   [clojure.string :as string]
   [clojure.tools.logging :as log]))

(def ^:private cookie-name "book-app")

;;-----------------------------------------------------------------------------
;; Request helpers
;;-----------------------------------------------------------------------------

(defn- request->str
  [request]
  (let [{:keys [uri scheme request-method server-name server-port query-string]} request
        email (:email (cookie/get request))]
    (format "{:user '%s', :method '%s', :url '%s://%s%s%s%s'}"
            (if (nil? email) "unknown" email)
            (string/lower-case (name request-method))
            (name scheme)
            server-name
            (if (= server-port 80) "" (str ":" server-port))
            uri
            (if (empty? query-string) "" (str "?" query-string)))))

(defn- public-path?
  "Is the request allowed to proceed even if not authorized/authenticated?"
  [request]
  (let [u (:uri request)]
    (or (.startsWith u "/bm/login")
        (.startsWith u "/bm/css")
        (.startsWith u "/bm/js")
        (.startsWith u "/bm/pix")
        (.startsWith u "/bm/bookmark/")
        (.startsWith u "/bm/api/join")
        (.startsWith u "/bm/api/auth"))))

(defn- authentic?
  "Is the user implied by the cookie authentic?"
  [request]
  (if-let [user (cookie/get request)]
    (db/authentic? (:email user) (:password user))
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

(defn- wrap-cookie-test
  "Makes sure the cookie is decryptable."
  [handler]
  (fn [request]
    (let [cookie (get-in (:cookies request) [cookie-name :value])]
      (cond (nil? cookie)
            (handler request)

            (try
              (let [data (read-string (crypto/decrypt cookie))]
                (or (nil? (:email data))
                    (nil? (:password data))))
              (catch Throwable t
                (log/error t)
                false))

            (do
              (log/info " - invalid cookie, resetting.")
              (-> (resp/redirect "/bm/login/")
                  (cookie/unset! request)))

            :else
            (handler request)))))

(defn- wrap-auth
  "Redirects request to the login page if not authenticated/authorized."
  [handler]
  (fn [request]
    (cond (authentic? request)
          (handler request)
          (public-path? request)
          (handler request)
          :else
          (do
            (log/info (format " - Denied access on %s, sending to /bm/login/."
                              (:uri request)))
            (resp/redirect "/bm/login/")))))

;;-----------------------------------------------------------------------------
;; Routes
;;-----------------------------------------------------------------------------

(defroutes main-routes

  (GET "/bm" [] (controller/redirect "/bm/"))

  (GET "/bm/" [:as request] (controller/home-page request))

  (GET "/bm/login" [] (controller/redirect "/bm/login/"))

  (GET "/bm/login/" [:as request] (controller/login-page request "/bm/"))

  (GET "/bm/logout/" [:as request] (controller/logout request "/bm/login/"))

  (GET "/bm/account/edit/" [:as request] (controller/account-edit-page request))

  (POST "/bm/bookmark/" [name addr tags cuid :as req] (controller/add-bookmark req name addr tags cuid))

  (POST "/bm/api/account/" [email password :as req] (controller/update-account req email password))

  (POST "/bm/api/auth/" [email password :as req] (controller/authorize req email password))

  (POST "/bm/api/join/" [email password :as req] (controller/join req email password))

  (POST "/bm/api/bookmark/" [name addr tags :as req] (controller/add-bookmark req name addr tags))

  (POST "/bm/api/search/" [terms :as request] (controller/search request terms))

  (route/resources "/")
  (route/not-found "<a href='/bm/'>Page not found.</a>"))

(def ^:private app (-> main-routes
                       (wrap-auth)
                       (wrap-cookie-test)
                       (wrap-request-logger)
                       (handler/site)))

;;-----------------------------------------------------------------------------
;; Server
;;-----------------------------------------------------------------------------

(defonce ^:private server (atom nil))

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
