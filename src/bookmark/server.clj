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



;;-----------------------------------------------------------------------------
;; Middleware
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

(defn- wrap-request-logger
  [handler]
  (fn [request]
    (log/info (request->str request))
    (handler request)))

(defn- wrap-auth
  [handler]
  (fn [request]
    (handler request)))

;;-----------------------------------------------------------------------------

(defn- with-cookie
  [document cookie-value request]
  (-> (resp/response document)
      (resp/set-cookie "book-app" cookie-value {:domain (:server-name request)
                                                :port (:server-port request)
                                                :path "/bm"
                                                :max-age max-age-seconds})))

(defn- as-html
  [data]
  (-> (resp/response data)
      (resp/content-type "text/html;charset=UTF-8")))

(defn- as-json
  [^APersistentMap data]
  (-> (resp/response (json/json-str data))
      (resp/content-type "application/json;charset=UTF-8")))

(defroutes main-routes
  ;;
  ;;
  ;;
  (GET "/bm" []
    (resp/redirect "/bm/"))
  ;;
  ;;
  ;;
  (GET "/bm/" [:as request]
    (let [c (:cookies request)]
      (log/info "GET /bm/ :: COOKIE:" c))
    (resp/resource-response "index.html" {:root "public/bm"}))
  ;;
  ;;
  ;;
  (GET "/bm/login/" [:as request]
    (let [c (:cookies request)]
      (log/info "GET /bm/login/ :: COOKIE:" c)
      (when-let [cookie (get-in c ["book-app" :value])]
        (log/info "  decrypted = " (crypto/decrypt cookie))))
    (-> (resp/response "<h1>Okay</h1>")
        (resp/content-type "text/html;charset=UTF-8")
        (resp/set-cookie "book-app" (crypto/encrypt "anonymous")
                         {:domain (:server-name request)
                          :port (:server-port request)
                          :path "/bm"
                          :max-age max-age-seconds})))
  ;;
  ;;
  ;;
  (GET "/bm/logout/" [:as request]
    (-> (resp/redirect "/bm/login/")
        (resp/set-cookie "book-app" ""
                         {:domain (:server-name request)
                          :port (:server-port request)
                          :path "/bm"
                          :max-age -1})))
  ;;
  ;;
  ;;
  (context "/bm/api" []
    (POST "/search/" [terms]
      (as-json (db/search terms))))
  ;;
  ;;
  ;;
  (route/resources "/")
  ;;
  ;;
  ;;
  (route/not-found "<a href='/bm/'>Page not found.</a>"))

(def ^:private app (-> main-routes
                       (wrap-request-logger)
                       (wrap-auth)
                       (handler/site)))

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
