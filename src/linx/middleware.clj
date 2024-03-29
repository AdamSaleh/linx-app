(ns linx.middleware
  ;;
  ;; Functionality invoked on every request/response.
  ;;
  (:require
   [linx.cookie :as cookie]
   [linx.model :as model]
   [clojure.string :as string]
   [clojure.tools.logging :as log]
   [ring.util.response :as response]))

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

(defn- authentic?
  [request]
  (let [user (cookie/get request)]
    (model/authentic? (:email user) (:password user))))

(defn- cookieless-redirect
  [request to]
  (log/info " - unsetting cookie")
  (-> (response/redirect to)
      (cookie/unset! request)))

(defn- auth
  [handler request public-path? redirect-to]
  (if (or (authentic? request)
          (public-path? request))
    (handler request)
    (cookieless-redirect request redirect-to)))

(defn cookie-test
  [handler request public-path? redirect-to]
  (if (or (cookie/valid? request) (public-path? request))
    (handler request)
    (cookieless-redirect request redirect-to)))

;;-----------------------------------------------------------------------------

(defn wrap-request-logger
  [handler]
  (log/info "Registering wrap-request-logger middleware")
  (fn [request]
    (let [u (:uri request)]
      ;;
      ;; Need a way to add a list of path fragments to omit
      ;;
      (when (not (or (.endsWith u "css")
                     (.endsWith u "js")
                     (.endsWith u "search/")))
        (log/info (request->str request))))
    (handler request)))

(defn wrap-auth
  [handler public-path? redirect-to]
  (log/info "Registering wrap-auth middleware")
  (fn [request]
    (auth handler request public-path? redirect-to)))

(defn wrap-cookie-test
  [handler public-path? redirect-to]
  (log/info "Registering wrap-cookie-test middleware")
  (fn [request]
    (cookie-test handler request public-path? redirect-to)))
