(defproject bookmark-web "0.1.0"
  :description "Simple Bookmarking Web App"
  :url "http://github.com/zentrope"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/data.json "0.1.2"]
                 [org.clojure/tools.logging "0.2.3"]
                 [compojure "1.1.1"]
                 [ring/ring-jetty-adapter "1.1.1"]
                 [swank-clojure/swank-clojure "1.4.2"]
                 [hiccup "1.0.0"]
                 [digest "1.3.0"]
                 [com.novemberain/monger "1.1.0"]
                 [ch.qos.logback/logback-classic "1.0.0"]]
  :run-aliases {:db-init bookmark.migration}
  :main bookmark.server)
