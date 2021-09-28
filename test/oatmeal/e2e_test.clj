(ns oatmeal.e2e-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as string]
            [clojure.test :refer [deftest testing is]]
            [me.raynes.fs :as rfs]
            [oatmeal.core :refer [execute-cmd]]
            [oatmeal.fs :refer [mkdirp]]
            [oatmeal.fs :as fs])
  (:import [java.nio.file FileAlreadyExistsException]))

(defn- oatmeal-cmd [env s]
  (with-out-str
    (execute-cmd env (string/split s #"\s+"))))

(defn- exists-in-dir [dir path-in-dir]
  (->> (str dir path-in-dir)
       io/file
       .exists))

(deftest e2etests-common
  (doseq [kind [:lib :app]]
    (testing (str "Making a new " (name kind) " project")
      (fs/with-tmp-dir d
        (let [exists (partial exists-in-dir d)]
          (testing "The directory already exists"
            (mkdirp (str d "/baz"))
            (testing "... exception is thrown"
              (is (thrown? FileAlreadyExistsException
                           (oatmeal-cmd {:oatmeal-dir (str d)}
                                        (str "create " (name kind) " baz"))))))
          (testing "It should create a directory called `foo`"
            (oatmeal-cmd {:oatmeal-dir (str d)}
                         (str "create " (name kind) " foo"))
            (testing "The project directory exists"
              (is (exists "/foo")))
            (testing "There is a Makefile"
              (is (exists "/foo/Makefile")))
            (testing "There is a main.lisp"
              (is (exists "/foo/main.lisp")))
            (testing "There is a package.lisp"
              (is (exists "/foo/package.lisp")))
            (testing "There is an ASDF file"
              (is (exists "/foo/foo.asd")))
            (when (= kind :app)
              (testing "There is a build.sh"
                (is (exists "/foo/build.sh")))
              (testing "Building it doesn't barf"
                (let [{:keys [exit out err]}
                      (shell/sh "make" :dir (str d "/foo"))]
                  (testing "Make succeeded"
                    (is (zero? exit))
                    (is (seq out))
                    (is (empty? err))
                    (testing "It built an executable"
                      (is (exists "/foo/foo"))))))
              (testing "Running it works"
                (let [{:keys [exit out err]}
                      (shell/sh "./foo" :dir (str d "/foo"))]
                  (testing "Execution succeeded"
                    (is (zero? exit))
                    (is (seq out))
                    (is (empty? err)))))
              ;; You are here: Add Quicklisp-related tests
              (testing "`make install`"
                (testing "with target dir defined"
                  (fs/with-tmp-dir bindir
                    (let [{:keys [exit err]}
                          (shell/sh "make" "install"
                                    :dir (str d "/foo")
                                    :env {"BINDIR" (str bindir)})]
                      (testing "Execution succeeded"
                        (is (zero? exit))
                        (is (empty? err)))
                      (let [foofile (io/file (str bindir "/foo"))]
                        (testing "Target executable exists"
                          (is (.exists foofile))
                          (testing "and is executable"
                            (is (rfs/executable? foofile))))))))
                (testing "with target dir not defined"
                  (let [{:keys [exit err]}
                        (shell/sh "make" "install"
                                  :dir (str d "/foo"))]
                    (testing "Execution failed as desired"
                      (is (not (zero? exit)))
                      (is (seq err))))))
              (testing "`make clean`"
                (let [{:keys [exit err]}
                      (shell/sh "make" "clean"
                                :dir (str d "/foo"))]
                  (testing "It succeeded"
                    (is (zero? exit))
                    (is (empty? err))
                    (is (not (exists "/foo/foo")))))))))
        (testing "Just creating the project files, but in a deeply nested path"
          (oatmeal-cmd {:oatmeal-dir (str d "/a/nested/sub/directory")}
                       (str "create " (name kind) " foo")))))))
