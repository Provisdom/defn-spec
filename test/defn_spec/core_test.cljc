(ns defn-spec.core-test
  #?(:cljs (:require-macros [defn-spec.core-test :refer [is-error-thrown is-exception-thrown without-asserts]]))
  (:require
    #?(:cljs [cljs.test :refer-macros [deftest is testing]]
       :clj  [clojure.test :refer [deftest is testing]])
    [clojure.spec.alpha :as s]
    [defn-spec.core :as ds]))

#?(:clj
   (defn- cljs-env?
     "Take the &env from a macro, and tell whether we are expanding into cljs."
     [env]
     (boolean (:ns env))))

#?(:clj
   (defmacro is-exception-thrown
     "(is (thrown-with-msg? ...)) for specified exceptions in Clojure/ClojureScript."
     [clj-exc-class cljs-exc-class re expr]
     (let [is (if (cljs-env? &env) 'cljs.test/is
                                   'clojure.test/is)
           exc-class (if (cljs-env? &env) cljs-exc-class
                                          clj-exc-class)]
       `(~is (~'thrown-with-msg? ~exc-class ~re ~expr)))))

#?(:clj
   (defmacro is-error-thrown
     "(is (thrown-with-msg? ...)) for general exceptions in Clojure/ClojureScript."
     [re expr]
     `(is-exception-thrown java.lang.Exception js/Error ~re ~expr)))

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
    (is-error-thrown #"did not conform to spec" (arity-1-fn "")))
  (testing "Return value is checked."
    (is-error-thrown #"did not conform to spec" (arity-1-fn -2)))

  (is (n-arity-fn 1))
  (is (n-arity-fn 1 2))
  (is (:doc (meta #'n-arity-fn)))
  (is-error-thrown #"did not conform to spec" (n-arity-fn 1 "2"))
  (is-error-thrown #"did not conform to spec" (n-arity-fn -1 0)))

#?(:clj (defmacro without-asserts [& body]
          (eval `(binding [s/*compile-asserts* false]
                   (macroexpand-1 (quote ~@body))))))

(deftest test-compile-asserts-false
  (without-asserts
    (ds/defn-spec no-asserts
      {::s/args (s/cat :a number? :b (s/? number?))}
      ([a] "a")
      ([a b] "a b")))
  (is (no-asserts "1")))

(defn instrumented-fn
  [x]
  (inc x))

(ds/fdef instrumented-fn
  :args (s/cat :x number?)
  :ret number?)

(deftest test-fdef
  (is (instrumented-fn 1))
  (is-error-thrown #"did not conform to spec" (instrumented-fn "")))
