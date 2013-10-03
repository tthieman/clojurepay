(ns clojurepay.views
  (:use [net.cgrand.enlive-html]
        sandbar.stateful-session
        [clojurepay.config :only [config]]
        clojurepay.auth)
  (:require [monger.collection :as mc]))

(defn redirect-to [location] {:status 302
                              :headers {"Location" location}})

(defsnippet navbar-link "public/templates/navbar-link.html" [:li] [id href text]
  [:li] (set-attr :id id)
  [:a] (do-> (set-attr :href href)
             (content text)))

(defsnippet navbar "public/templates/navbar.html" [:.navbar] []
  [:#navbar-right] (if (logged-in?)
                     (append (navbar-link "logout" "/logout" "Log Out"))
                     (append (navbar-link "login" "/login" "Log In"))))

(defsnippet footer "public/templates/footer.html" [root] [] identity)

(defsnippet alert "public/templates/alert.html" [:.alert] [msg class]
  [:.alert] (do-> (add-class (str "alert-" (if (nil? msg) "hidden" class)))
                  (content msg)))

(deftemplate base-template "public/templates/base.html" [body-content]
  [:#body-content] (content body-content)
  [:#navbar] (substitute (navbar))
  [:#footer] (substitute (footer)))

(defsnippet signup-form "public/templates/signup-form.html" [:form] [form-action msg]
  [:form] (do-> (prepend (alert msg "warning"))
                (set-attr :action form-action)))

(defsnippet login-form "public/templates/login-form.html" [:form] [form-action msg]
  [:form] (do-> (prepend (alert msg "warning"))
                (set-attr :action form-action)))

(defn index-redirect []
  (if (logged-in?)
    (redirect-to "/circles")
    (redirect-to "/signup")))

(defn session-print-view [] (base-template (session-get :logged-in)))

(defn signup-view
  ([] (signup-view nil))
  ([msg] (base-template (signup-form "do-signup" msg))))

(defn login-view
  ([] (login-view nil))
  ([msg] (base-template (login-form "do-login" msg))))

(defn do-signup-view [params]
  (if (email-exists? (:email params))
    (signup-view "A user with this email already exists.")
    (try
      (save-new-user (:name params) (:email params) (:password params))
      (login-user (:email params))
      (redirect-to "/")
      (catch Exception e
        (signup-view "There was an error creating your account. Please try again.")))))

(defn do-login-view [params]
  (if (password-is-correct? (:email params) (:password params))
    (do
      (login-user (:email params))
      (redirect-to "/"))
    (login-view "Incorrect email or password.")))

(defn logout-view [session]
  (destroy-session!)
  (redirect-to "/"))
