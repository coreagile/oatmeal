(ns oatmeal.build
  (:require [clojure.java.io :as io]
            [clostache.parser :refer [render]]
            [me.raynes.fs :refer [chmod]]
            [oatmeal.fs :as fs])
  (:import [java.nio.file FileAlreadyExistsException]))

(defn make-lib [env projname]
  (let [tldir (fs/lisp-toplevel-dir env)
        target (str tldir "/" projname)]
    (when (.exists (io/file target))
      (throw (FileAlreadyExistsException.
              (format "Directory '%s' already exists; not overwriting."
                      target))))
    (fs/mkdirp target)
    (fs/mkdirp (str target "/src"))
    (spit (str target "/Makefile") "")
    (spit (str target "/" projname ".asd") "")
    (spit (str target "/src/main.lisp") "(format t \"Hello World~%\")\n")
    (spit (str target "/src/package.lisp") "")
    (println "LIB" projname "in directory" tldir)))

(defn render-and-write [projname target-dir target-file src-file]
  (spit (str target-dir "/" target-file)
        (render (fs/resource-file src-file)
                {:progname projname})))

(defn make-app [env projname]
  (let [tldir (fs/lisp-toplevel-dir env)
        target (str tldir "/" projname)
        render-file (partial render-and-write projname target)]
    (when (.exists (io/file target))
      (throw (FileAlreadyExistsException.
              (format "Directory '%s' already exists; not overwriting."
                      target))))
    (fs/mkdirp target)
    (fs/mkdirp (str target "/src"))
    (render-file "Makefile" "app/Makefile")
    (render-file "build.sh" "app/build.sh")
    (render-file "src/main.lisp" "app/main.lisp")
    (render-file "src/package.lisp" "app/package.lisp")
    (render-file (str projname ".asd") "app/app.asd")
    (chmod "+x" (str target "/build.sh"))
    (println "APP" projname "in directory" tldir)))
