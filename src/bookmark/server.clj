(ns bookmark.server
  (:gen-class)
  (:use
   compojure.core)
  (:require
   [bookmark.controller :as controller]
   [bookmark.middleware :as middleware]
   [compojure.route :as route]
   [compojure.handler :as handler]
   [ring.adapter.jetty :as jetty]
   [swank.swank :as swank]
   [clojure.tools.logging :as log]))

;;-----------------------------------------------------------------------------

(defroutes main-routes
  (GET "/bm" [] (controller/redirect "/bm/"))
  (GET "/bm/" [:as req] (controller/home-page req))
  (GET "/bm/login" [] (controller/redirect "/bm/login/"))
  (GET "/bm/login/" [:as req] (controller/login-page req "/bm/"))
  (GET "/bm/logout/" [:as req] (controller/logout req "/bm/login/"))
  (GET "/bm/account/edit/" [:as req] (controller/account-edit-page req))
  (POST "/bm/bookmark/" [name addr tags cuid :as req] (controller/add-bookmark req name addr tags cuid))
  (POST "/bm/api/account/" [email password :as req] (controller/update-account req email password))
  (POST "/bm/api/auth/" [email password :as req] (controller/authorize req email password))
  (POST "/bm/api/join/" [email password :as req] (controller/join req email password))
  (POST "/bm/api/bookmark/" [name addr tags :as req] (controller/add-bookmark req name addr tags))
  (DELETE "/bm/api/bookmark/:id/" [id :as req] (controller/delete-bookmark req id))
  (POST "/bm/api/search/" [terms :as req] (controller/search req terms))
  (route/resources "/")
  (route/not-found "<a href='/bm/'>Page not found.</a>"))

(defn- public-path?
  [request]
  (let [u (:uri request)]
    (or (.startsWith u "/bm/login")
        (.startsWith u "/bm/css")
        (.startsWith u "/bm/js")
        (.startsWith u "/bm/pix")
        (.startsWith u "/bm/bookmark/")
        (.startsWith u "/bm/api/join")
        (.startsWith u "/bm/api/auth"))))

(defn- start-swank
  []
  (log/info "Starting swank server.")
  (swank/start-server :port 4005))

(defn- stop-swank
  []
  (log/info "Stopping swank server.")
  (swank/stop-server))

(def ^:private app (-> main-routes
                       (middleware/wrap-auth public-path? "/bm/login/")
                       (middleware/wrap-cookie-test public-path? "/bm/login/")
                       (middleware/wrap-request-logger)
                       (handler/site)))

(defonce ^:private server (atom nil))

(defn- start
  ([opts]
     (log/info "Starting web server.")
     (reset! server (jetty/run-jetty
                     (var app)
                     (merge {:port 8087 :join? false} opts))))
  ([]
     (start {})))

(defn- stop
  []
  (log/info "Shutting down web server.")
  (when (not (nil? @server))
    (.stop @server)
    (reset! server nil)))

(defn -main
  [& args]
  (start-swank)
  (start {:join? true}))
