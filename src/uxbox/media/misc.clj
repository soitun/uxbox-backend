;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.media.misc
  "A local filesystem storage implementation."
  (:require [promesa.core :as p]
            [cuerdas.core :as str]
            [buddy.core.codecs :as codecs]
            [buddy.core.codecs.base64 :as b64]
            [buddy.core.nonce :as nonce]
            [buddy.core.hash :as hash]
            [uxbox.media.proto :as pt]
            [uxbox.media.impl :as impl]
            [uxbox.media.executor :as exec])
  (:import java.io.InputStream
           java.io.OutputStream
           java.nio.file.Path
           java.nio.file.Files
           org.apache.commons.io.FilenameUtils
           org.apache.commons.io.IOUtils))

;; --- Prefixed Storage

(defrecord PrefixedPathStorage [storage prefix]
  pt/IStorage
  (-save [_ path content]
    (let [^Path path (pt/-path path)
          ^Path path (.resolve ^Path prefix path)]
      (pt/-save storage path content)))

  (-delete [_ path]
    (pt/-delete storage path))

  (-exists? [this path]
    (pt/-exists? storage path))

  pt/ILocalStorage
  (-lookup [_ path]
    (pt/-lookup storage path)))

(defn prefixed
  "Create a composed storage instance that automatically prefixes
  the path when content is saved. For the rest of methods it just
  relies to the underlying storage.

  This is usefull for atomatically add sertain prefix to some
  uploads."
  [storage prefix]
  (let [prefix (pt/-path prefix)]
    (->PrefixedPathStorage storage prefix)))

;; --- Hashed Storage

(defn- concat-path
  [path & others]
  (reduce #(FilenameUtils/concat %1 %2) path others))

(defn- generate-path
  [^Path path]
  (let [name (str (.getFileName path))
        hash (-> (nonce/random-nonce 128)
                 (hash/blake2b-256)
                 (b64/encode true)
                 (codecs/bytes->str))
        tokens (re-seq #"[\w\d\-\_]{3}" hash)
        path-tokens (take 6 tokens)
        rest-tokens (drop 6 tokens)
        path (apply concat-path path-tokens)
        frest (apply str rest-tokens)]
    (pt/-path
     (concat-path path frest name))))

(defrecord HashedStorage [storage]
  pt/IStorage
  (-save [_ path content]
    (let [^Path path (pt/-path path)
          ^Path path (generate-path path)]
      (pt/-save storage path content)))

  (-delete [_ path]
    (pt/-delete storage path))

  (-exists? [this path]
    (pt/-exists? storage path))

  pt/ILocalStorage
  (-lookup [_ path]
    (pt/-lookup storage path)))

(defn hashed
  "Create a composed storage instance that uses random
  hash based directory tree distribution for the final
  file path.

  This is usefull when you want to store files with
  not predictable uris."
  [storage]
  (->HashedStorage storage))

