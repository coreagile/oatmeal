(defsystem :{{projname}}
  :version "0.0.1"
  :description "FIXME"
  :author "FIXME"
  :license "FIXME"
  :serial t
  :in-order-to ((asdf:test-op (asdf:test-op :{{projname}}/test)))
  :depends-on (:arrows)
  :components ((:module "src"
                :serial t
                :components ((:file "package")
                             (:file "main" :depends-on ("package"))))))

(defsystem :{{projname}}/test
  :description "FIXME"
  :author "FIXME"
  :license "FIXME"
  :depends-on (:{{projname}} :1am)
  :serial t
  :components ((:module "test"
                :serial t
                :components ((:file "package")
                             (:file "test"))))
  :perform (asdf:test-op (op system)
                         (funcall (read-from-string "{{projname}}.test:run-tests"))))
