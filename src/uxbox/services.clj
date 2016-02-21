(ns uxbox.services
  "Main namespace for access to all uxbox services."
  (:require [suricatta.core :as sc]
            [catacumba.serializers :as sz]
            [catacumba.impl.executor :as exec]
            [clj-uuid :as uuid]
            [uxbox.persistence :as up]
            [uxbox.services.core :as usc]
            [uxbox.services.auth]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Impl.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- encode
  [data]
  (-> (sz/encode data :transit+json)
      (sz/bytes->str)))

(defn- insert-txlog
  [conn data]
  (let [sql (str "INSERT INTO txlog (payload) VALUES (?)")
        sqlv [sql (encode data)]]
    (sc/execute conn sqlv)))

(defn- handle-novelty
  [data]
  (with-open [conn (up/get-conn)]
    (sc/atomic conn
      (let [result (usc/-novelty conn data)]
        (insert-txlog conn data)
        result))))

(defn- handle-query
  [data]
  (with-open [conn (up/get-conn)]
    (sc/atomic conn
      (usc/-query conn data))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public Api
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn novelty
  [data]
  (exec/submit (partial handle-novelty data)))

(defn query
  [data]
  (exec/submit (partial handle-query data)))