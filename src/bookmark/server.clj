(ns bookmark.server
  (:gen-class)
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

(defn- cookie
  [request]
  "Returns an evalution of the decrypted cookie, if it exists."
  (if-let [cookie (get-in (:cookies request) [cookie-name :value])]
    (read-string (crypto/decrypt cookie))
    nil))

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

(defn- request->str
  [request]
  (let [{:keys [uri scheme request-method server-name server-port query-string]} request
        email (:email (cookie request))]
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
        (.startsWith u "/bm/html")
        (.startsWith u "/bm/api/auth"))))

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

(defn- wrap-cookie-test
  "Makes sure the cookie is decryptable."
  [handler]
  (fn [request]
    (let [cookie (get-in (:cookies request) [cookie-name :value])]
      (cond (nil? cookie)
            (handler request)

            (let [data (read-string (crypto/decrypt cookie))]
              (or (nil? (:email data))
                  (nil? (:password data))))

            (do
              (log/info " - invalid cookie, resetting.")
              (-> (resp/redirect "/bm/login/")
                  (unset-cookie! request)))

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
            (log/info (format " - Denied access on %s, sending to /bm/login/." (:uri request)))
            (resp/redirect "/bm/login/")))))

;;-----------------------------------------------------------------------------
;; Renderers
;;-----------------------------------------------------------------------------

(defn- as-json
  [data]
  (-> (resp/response (json/json-str data))
      (resp/content-type "application/json;charset=UTF-8")))

;;-----------------------------------------------------------------------------
;; Controllers
;;-----------------------------------------------------------------------------

(defn- handle-page
  [request page]
  (-> (resp/resource-response page {:root "public/bm"})
      (resp/content-type "text/html; charset=UTF-8")))

(defn- handle-home
  [request]
  (handle-page request "html/index.html"))

(defn- handle-login
  [request]
  (if (authentic? request)
    (resp/redirect "/bm/")
    (handle-page request "html/login.html")))

(defn- handle-logout
  [request]
  (-> (resp/redirect "/bm/login/")
      (unset-cookie! request)))

(defn- handle-auth
  [request email password]
  (log/info " - request to auth." {:email email :password password})
  (if-let [user (db/authentic? email password)]
    (do
      (log/info " - valid auth.")
      (-> (resp/response "")
          (cookie! request (crypto/encrypt (str user)))))
    (do
      (log/info " - invalid auth.")
      (status-response 403))))

(defn- handle-search
  [terms]
  (as-json (db/search terms)))

(defn- handle-new-bookmark
  [request name addr tags]
  (try
    (do
      (db/bookmark! (:email (cookie request)) name addr (map string/trim (string/split tags #"[,]")))
      (status-response 201))
    (catch Throwable t
      (log/error (str " - " t))
      (status-response 400))))

;;-----------------------------------------------------------------------------
;; Routes
;;-----------------------------------------------------------------------------

(defroutes main-routes
  (GET "/bm" [] (resp/redirect "/bm/"))
  (GET "/bm/" [:as request] (handle-home request))
  (GET "/bm/login" [] (resp/redirect "/bm/login/"))
  (GET "/bm/login/" [:as request] (handle-login request))
  (GET "/bm/logout/" [:as request] (handle-logout request))
  (POST "/bm/api/auth/" [email password :as req] (handle-auth req email password))
  (POST "/bm/api/bookmark/" [name addr tags :as req] (handle-new-bookmark req name addr tags))
  (POST "/bm/api/search/" [terms] (handle-search terms))
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
