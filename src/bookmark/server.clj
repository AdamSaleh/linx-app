(ns bookmark.server
  (:gen-class)
  (:import
   [clojure.lang APersistentMap])
  (:use
   compojure.core)
  (:require
   [bookmark.db :as db]
   [clojure.data.json :as json]
   [compojure.route :as route]
   [compojure.handler :as handler]
   [ring.adapter.jetty :as jetty]
   [ring.util.response :as resp]
   [clojure.tools.logging :as log]))

(def ^:private max-age-seconds (* 30 24 60 60)) ;; 30 days

(defn- with-cookie
  [document cookie-value request]
  (log/info request)
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

(defroutes app
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
      (log/info "GET /bm/login/ :: COOKIE:" c))
    (-> (resp/response "<h1>Okay</h1>")
        (resp/content-type "text/html;charset=UTF-8")
        (resp/set-cookie "book-app" "anonymous" {:domain (:server-name request)
                                                 :port (:server-port request)
                                                 :path "/bm"
                                                 :max-age max-age-seconds})))
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

(def ^:private server (atom nil))

(defn- start
  ([opts]
     (log/info "Starting server.")
     (reset! server (jetty/run-jetty
                     (handler/site (var app))
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
