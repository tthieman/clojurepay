(ns clojurepay.model
  (:use [clojurepay.util :only [swap-keys random-string assert-args]])
  (:require [monger.collection :as mc]
            [monger.query :as mq]
            [clj-time.core :as time])
  (:import [org.bson.types ObjectId]))

(defprotocol StaticParams
  (opts [this key]))

(defprotocol PersistableRecordCollection
  (fetch-collection [this record-type query sort]))

(defprotocol Document
  (fetch [this id])
  (delete [this])
  (parse [this doc])
  (insert [this args-map])
  (update [this query]))

(defrecord User []

  StaticParams
  (opts [this key]
    (let [config {:coll "user"}]
      (get config key)))

  Document
  (fetch [this _id]
    (let [user-doc (mc/find-map-by-id (opts this :coll) _id)
          new-user (assoc (->User) :_id _id)]
      (when user-doc
        (merge new-user user-doc))))

  (delete [this]
    (let [id (:_id this)]
      (when id (mc/remove (opts this :coll) {:_id (ObjectId. (str id))}))))

  (parse [this doc] (merge (->User) doc))

  (insert [this {:keys [email name password]}]
    (assert-args [email name password])
    (mc/insert (opts this :coll)
               {:_id (clojure.string/lower-case email)
                :active false
                :name name
                :proper-email email
                :password password}))

  (update [this query]
    (mc/update-by-id (opts this :coll)
                     (:_id this)
                     query)
    (fetch (->User) (:_id this))))

(defrecord Circle []

  StaticParams
  (opts [this key]
    (let [config {:coll "circle"}]
      (get config key)))

  Document
  (fetch [this _id]
    (let [circle-doc (mc/find-map-by-id (opts this :coll) (ObjectId. (str _id)))
          new-circle (assoc (->Circle) :_id _id)]
      (when circle-doc
        (merge new-circle circle-doc))))

  (delete [this]
    (let [id (:_id this)]
      (when id (mc/remove (opts this :coll) {:_id (ObjectId. (str id))}))))

  (parse [this doc] (merge (->Circle) doc))

  (insert [this {:keys [name owner-doc]}]
    (assert-args [name owner-doc])
    (let [circle-id (ObjectId. )
          invite-code (random-string 20)
          now (time/now)]
      (mc/insert (opts this :coll)
                 {:_id circle-id
                  :name name
                  :owner (-> owner-doc
                             (select-keys [:_id :name])
                             (swap-keys :_id :id))
                  :users [(-> owner-doc
                              (select-keys [:_id])
                              (swap-keys :_id :id))]
                  :invite_code invite-code
                  :created now
                  :updated now})))

  (update [this query]
    (mc/update-by-id (opts this :coll)
                     (:_id this)
                     query)
    (fetch (->Circle) (:_id this))))

(defrecord RecordCollection []
  PersistableRecordCollection
  (fetch-collection [this base-record query sort]
    (let [coll (opts base-record :coll)
          docs (mq/with-collection coll (mq/find query) (mq/sort sort))]
      (map #(parse base-record %) docs))))
