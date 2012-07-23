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

(defn- as-json
  [^APersistentMap data]
  (-> (resp/response (json/json-str data))
      (resp/content-type "application/json;charset=UTF-8")))

(defroutes app
  (GET "/bm" []
    (resp/redirect "/bm/"))
  (GET "/bm/" []
    (resp/resource-response "index.html" {:root "public/bm"}))
  (context "/bm/api" []
    (POST "/search/" [terms]
      (as-json (db/search terms))))
  (route/resources "/")
  (route/not-found "<a href='/bm/'>Page not found.</a>"))

(def ^:private server (atom nil))

(defn- start
  ([opts]
     (reset! server (jetty/run-jetty
                     (handler/api (var app))
                     (merge {:port 8087 :join? false} opts))))
  ([]
     (start {})))

(defn- stop
  []
  (when (not (nil? @server))
    (.stop @server)
    (reset! server nil)))

(defn -main
  [& args]
  (log/info "Hello, World!")
  (start {:join? true}))
