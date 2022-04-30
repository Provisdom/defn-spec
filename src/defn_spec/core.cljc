(ns defn-spec.core
  (:require
    [clojure.spec.alpha :as s]
    #?(:clj  [clojure.spec.test.alpha :as st]
       :cljs [cljs.spec.test.alpha :as st])))

#?(:clj
   (defn qualify-symbol
     [sym-name]
     (symbol (str *ns*) (str sym-name))))

#?(:clj
   (defn platform
     [env]
     (if (:ns env) :cljs :clj)))

;; From https://github.com/borkdude/speculative/blob/master/src/speculative/impl.cljc
#?(:clj (defmacro ?
          [& {:keys [cljs clj]}]
          (if (contains? &env '&env)
            `(if (:ns ~'&env) ~cljs ~clj)
            (if #?(:clj (:ns &env) :cljs true)
              cljs
              clj))))

#?(:clj
   (def *orchestra?
     (delay
       (try
         (require 'orchestra.spec.test)
         true
         (catch Exception _ false)))))

#?(:clj
   (defmacro instrument*
     [symbol]
     (let [instrument-sym (case [@*orchestra? (platform &env)]
                            [true :clj]
                            'orchestra.spec.test/instrument
                            [true :cljs]
                            'orchestra-cljs.spec.test/instrument
                            [false :clj]
                            'clojure.spec.test.alpha/instrument
                            [false :cljs]
                            'cljs.spec.test.alpha/instrument)]
       `(~instrument-sym ~symbol))))

#?(:clj
   (defmacro fdef
     "Exact same parameters as `s/fdef`. Automatically enables instrumentation
     for `fn-sym` when `s/*compile-asserts*` is true."
     [fn-sym & specs]
     `(let [r# (s/fdef ~fn-sym ~@specs)]
        ~@(when s/*compile-asserts*
            [`(instrument* '~(qualify-symbol fn-sym))])
        r#)))

(s/def ::defn-args
  (s/cat :name simple-symbol?
         :docstring (s/? string?)
         :meta (s/? map?)
         :bs (s/* any?)))

#?(:clj
   (defn- defn-spec-form
     [args]
     (let [{:keys [name meta]} (s/conform ::defn-args args)
           args-spec (::s/args meta)
           ret-spec (::s/ret meta)
           fn-spec (::s/fn meta)
           fdef-sym (if (false? (::instrument? meta)) `s/fdef `fdef)]
       `(do
          (defn ~@args)
          (~fdef-sym ~name
            ~@(when-let [s args-spec] [:args s])
            ~@(when-let [s ret-spec] [:ret s])
            ~@(when-let [s fn-spec] [:fn s]))))))

#?(:clj
   (defmacro defn-spec
     "Exact same parameters as `defn`. You may optionally include `::s/args`
     and/or `::s/ret` in your function's attr-map to have the args and/or return
     value of your function checked with `s/assert`.

     Setting `s/*compile-asserts*` to `false` will result in a regular function
     definition."
     {:arglists '([name doc-string? attr-map? [params*] prepost-map? body]
                  [name doc-string? attr-map? ([params*] prepost-map? body) + attr-map?])}
     [& args]
     (if s/*compile-asserts*
       (defn-spec-form args)
       `(defn ~@args))))

#?(:clj
   (s/fdef defn-spec
     :args ::defn-args
     :ret any?))