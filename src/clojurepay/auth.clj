(ns clojurepay.auth
  (:use [clojurewerkz.scrypt.core :as sc]
        [clojurepay.config :only [config]]
        sandbar.stateful-session)
  (:require [monger.collection :as mc]))

(def lcase clojure.string/lower-case)

(defn email-exists? [email]
  (let [l-email (clojure.string/lower-case email)]
    (pos? (mc/count "user" {:_id l-email}))))

(defn encrypt-password [password] (sc/encrypt password 16384 8 1))

(defn password-is-correct? [email password]
  (let [user-rec (mc/find-map-by-id "user" (lcase email))]
    (if (empty? user-rec)
      false
      (sc/verify password (:password user-rec)))))

(defn logged-in? []
  (let [l-email (session-get :user)]
    (if (nil? l-email)
      false
      (sc/verify (str l-email (:app_secret config)) (session-get :token)))))

(defn login-user
  "Add successful user auth info to current session."
  [email]
  (let [l-email (lcase email)]
    (session-put! :user l-email)
    (session-put! :token (sc/encrypt (str l-email (:app_secret config)) 16384 8 1))))

(defn save-new-user [name email password]
  (if (email-exists? email)
    (throw (Exception. "This email is taken, please choose another."))
    (mc/insert "user" {:_id (lcase email)
                       :name name
                       :proper-email email
                       :password (encrypt-password password)})))
