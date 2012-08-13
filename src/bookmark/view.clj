(ns bookmark.view
  (:require
   [bookmark.cookie :as cookie])
  (:use hiccup.core
        hiccup.page))

(defn- server
  [request]
  (let [{:keys [scheme server-name server-port]} request]
    (str (name scheme) "://" server-name
         (if (= server-port 80) "" (str ":" server-port)))))

(defn- bookmarklet-script
  [request]
  (let [s (server request)
        c (cookie/raw request)]
    (str "javascript:(function(){"
         "var d=document;"
         "var j=d.createElement('script');"
         "j.id='a1-code';"
         "j.setAttribute('uid','" c "');"
         "j.setAttribute('hb','" s "');"
         "j.src='" s "/bm/js/bookmarklet.js" "';"
         "d.body.appendChild(j);"
         "})();")))

(defn- bookmarklet
  [request]
  [:a {:title "Linx Bookmarklet" :href (bookmarklet-script request)} "linx"])

(def ^:private stock-js ["http://code.jquery.com/jquery-1.7.2.min.js"])

(def ^:private stock-css ["/bm/css/reset.css"
                          "/bm/css/bm.css"])
(defn- layout
  [css js content]
  (html5
   [:head
    [:title "Bookmarks"]
    [:link {:rel "apple-touch-icon" :href "/bm/pix/favicon.ico"}]
    [:link {:rel "shortcut icon" :href "/bm/pix/favicon.ico"}]
    [:link {:rel "icon" :type "image/vnd.microsoft.icon" :href "/bm/pix/favicon.ico"}]
    (apply include-js stock-js)
    (apply include-css stock-css)
    (when (not (empty? js))
      (include-js js))
    (when (not (empty? css))
      (include-css css))]
   [:body content]))

(defn account-page
  [request user]
  (layout
   ""
   "/bm/js/account.js"
   [:div.content
    [:div.header
     [:h1 "update account info"]]
    [:div.form
     [:form#account-form
      [:label {:for "email"}
       [:span.prompt "Email:"]
       ;; Disabling the email widget until we make sure all related bookmarks
       ;; get linked properly.
       [:span.widget [:input#email {:disable "true" :type "text" :value (:email user)} ]]]
      [:label {:for "new-pass"}
       [:span.prompt "New password:"]
       [:span.widget [:input#new-pass {:type "password" :value ""}]]]
      [:label {:for "confirm"}
       [:span.prompt "Confirm password:"]
       [:span.widget [:input#confirm {:type "password" :value ""}]]]
      [:div#bm-form-errors.form-errors]
      [:div.bm-form-buttons
       [:button#update "update"]
       [:button#cancel "cancel"]]]]
    [:div.footer
     [:p "&copy; 2012 Zentrope"]
     [:p [:a {:href "/bm/logout/"} "sign out"]]]]))

(defn login-page
  [request]
  (layout
   "/bm/css/login.css"
   "/bm/js/login.js"
   [:body
    [:div.content
     [:div.header
      [:h1 "bookmarks"]]
     ;;
     [:div#login-area
      ;;
      [:div#bm-error-wrapper
       [:div#bm-login-errors
        [:p#bm-error-pane "We don't recognize your account. Try again?"]]]
      ;;
      [:div#login-form.bm-form
       [:h1 "Sign in to Boomarks App"]
       [:form
        [:label {:for "bm-user"}
         [:span.prompt "Email:"]
         [:span.widget
          [:input#bm-user {:type "text"}]]]
        [:label {:for "bm-pass"}
         [:span.prompt "Password:"]
         [:span.widget
          [:input#bm-pass {:type "password"}]]]
        [:label#confirmer {:for "join-confirm" :style "display: none;"}
         [:span.prompt "Confirm"]
         [:span.widget [:input#join-confirm {:type "password"}]]]
        [:div.form-buttons
         [:button#bm-login "log in"]
         [:button#bm-join "join"]]]]]
     ;;
     [:div.footer
      [:p "&copy; 2012 Zentrope"]]]]))

(defn home-page
  [request]
  (html5
   [:head
    [:title "Bookmarks"]
    [:link {:rel "apple-touch-icon" :href "/bm/pix/favicon.ico"}]
    [:link {:rel "shortcut icon" :href "/bm/pix/favicon.ico"}]
    [:link {:rel "icon" :type "image/vnd.microsoft.icon" :href "/bm/pix/favicon.ico"}]
    (include-js "http://code.jquery.com/jquery-1.7.2.min.js"
                "/bm/js/bm.js")
    (include-css "/bm/css/reset.css"
                 "/bm/css/bm.css")]
   [:body
    [:div.content
     ;;
     [:div.header
      [:h1 "bookmarks"]
      [:h6 (:email (cookie/get request))]]
     ;;
     [:div.bookmarklet
      [:div.bookmarklet-link
       (bookmarklet request)]
      [:div.bookmarklet-help
       "Drag to your bookmark bar."]]
     ;;
     [:div.bm-tool
      [:div.bm-functions
       [:button#bm-new "new bookmark"]]
      [:div.bm-search
       [:input#search-terms {:type "text"}]]
      [:div#bm-form.form {:style "display: none;"}
       [:form#bm-form-itself
        [:label {:for "bm-url"}
         [:span.prompt "Bookmark URL:"]
         [:span.widget [:input#bm-url {:type "text"}]]]
        [:label {:for "bm-title"}
         [:span.prompt "Description:"]
         [:span.widget [:input#bm-title {:type "text"}]]]
        [:label {:for "bm-tags"}
         [:span.prompt "Tags:"]
         [:span.widget [:input#bm-tags {:type "text"}]]]
        [:div#bm-form-errors.form-errors]
        [:div.bm-form-buttons
         [:button#bm-add "add"]
         [:button#bm-cancel "cancel"]]]]]
     ;;
     [:div.bm-list
      [:table#bm-table
       [:tr#bm-table-header
        [:th {:width "40%"} "bookmark"]
        [:th {:width "30%"} "address"]
        [:th {:width "24%"} "tags"]
        [:th {:width "6%"} "id"]]]]
     ;;
     [:div.footer
      [:p "&copy; 2012 Zentrope"]
      [:p
       [:a {:href "/bm/logout/"} "sign out"]
       " | "
       [:a {:href "/bm/account/edit/"} "account"]]]]]))
