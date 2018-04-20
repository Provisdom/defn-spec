(ns defn-spec.core-test
  (:require
    [clojure.test :refer :all]
    [clojure.spec.alpha :as s]
    [defn-spec.core :as ds])
  (:import (clojure.lang ExceptionInfo)))

(s/check-asserts true)

(ds/defn-spec arity-1-fn
  {::s/args (s/cat :x int?)
   ::s/ret  nat-int?}
  [x]
  (inc x))

(ds/defn-spec n-arity-fn
  "docstring"
  {::s/args (s/cat :x int? :y (s/? int?))
   ::s/ret  nat-int?}
  ([x] (n-arity-fn x 0))
  ([x y]
   (+ x y)))

(deftest test-function-calls
  (is (arity-1-fn 1))
  (testing "args vec is checked"
    (is (thrown? ExceptionInfo (arity-1-fn ""))))
  (testing "Return value is checked."
    (is (thrown? ExceptionInfo (arity-1-fn -2))))

  (is (n-arity-fn 1))
  (is (n-arity-fn 1 2))
  (is (:doc (meta #'n-arity-fn)))
  (is (thrown? ExceptionInfo (n-arity-fn 1 "2")))
  (is (thrown? ExceptionInfo (n-arity-fn -1 0))))

(deftest test-compile-asserts-false
  (binding [s/*compile-asserts* false]
    (is (= '(clojure.core/defn no-asserts "doc" [a] "a")
           (macroexpand-1 '(ds/defn-spec no-asserts "doc" [a] "a"))))
    (is (= '(clojure.core/defn no-asserts ([a] "a") ([a b] "a b"))
           (macroexpand-1 '(ds/defn-spec no-asserts ([a] "a") ([a b] "a b")))))))

(defn instrumented-fn
  [x]
  (inc x))

(ds/fdef instrumented-fn
         :args (s/cat :x number?)
         :ret number?)

(deftest test-fdef
  (is (instrumented-fn 1))
  (is (thrown? ExceptionInfo (instrumented-fn ""))))