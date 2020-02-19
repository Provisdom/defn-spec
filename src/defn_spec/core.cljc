(ns defn-spec.core
  (:require
    [clojure.spec.alpha :as s]
    [defn-spec.defn-args :as defn-args]
    #?(:clj  [clojure.spec.test.alpha :as st]
       :cljs [cljs.spec.test.alpha :as st])))

(defn assert*
  [kind fn-name spec x]
  (if (s/valid? spec x)
    x
    (let [ed (merge (assoc (s/explain-data spec x)
                      ::s/failure :assertion-failed))]
      (throw (ex-info
               (str
                 (case kind
                   :args (str "Call to "
                              fn-name
                              " did not conform to spec:")
                   :ret (str "Return value from "
                             fn-name
                             " did not conform to spec:"))
                 "\n"
                 (with-out-str (s/explain-out ed)))
               ed)))))

#?(:clj
   (defn qualify-symbol
     [sym-name]
     (symbol (str *ns*) (str sym-name))))

#?(:clj
   (defn- defn-spec-form
     [args]
     (let [{:keys [name docstring meta bs]} (s/conform ::defn-args/defn-args args)
           qualified-name (qualify-symbol name)
           args-spec (or (::s/args meta) (:cljs.spec.alpha/args meta))
           ret-spec (or (::s/ret meta) (:cljs.spec.alpha/ret meta))
           body (let [[arity-type conformed-bodies] bs]
                  (case arity-type
                    :arity-1 (s/unform ::defn-args/args+body conformed-bodies)
                    :arity-n (mapv (partial s/unform ::defn-args/args+body) (:bodies conformed-bodies))))
           inner-fn-name (symbol (str name "-impl"))
           args-sym (gensym "args")
           result-sym (gensym "result")]
       `(let [fn# (defn ~@args)
              meta# (meta fn#)
              new-fn# (defn ~name
                        ~@(when docstring [docstring])
                        ~@(when meta [meta])
                        [& ~args-sym]
                        ~@(when args-spec [`(assert* :args '~qualified-name ~args-spec ~args-sym)])
                        (let [~result-sym (apply (fn ~inner-fn-name ~@body) ~args-sym)]
                          ~@(when ret-spec [`(assert* :ret '~qualified-name (s/spec ~ret-spec) ~result-sym)])
                          ~result-sym))]
          (alter-meta! (var ~(symbol name)) merge meta#)
          new-fn#))))

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
     :args ::defn-args/defn-args
     :ret any?))

;; From https://github.com/borkdude/speculative/blob/master/src/speculative/impl.cljc
#?(:clj (defmacro ?
          [& {:keys [cljs clj]}]
          (if (contains? &env '&env)
            `(if (:ns ~'&env) ~cljs ~clj)
            (if #?(:clj (:ns &env) :cljs true)
              cljs
              clj))))

#?(:clj
   (defmacro instrument*
     [symbol]
     `(? :clj
         (clojure.spec.test.alpha/instrument ~symbol)
         :cljs
         (cljs.spec.test.alpha/instrument ~symbol))))

#?(:clj
   (defmacro fdef
     "Exact same parameters as `s/fdef`. Automatically enables instrumentation
     for `fn-sym` when `s/*compile-asserts*` is true."
     [fn-sym & specs]
     `(let [r# (s/fdef ~fn-sym ~@specs)]
        ~@(when s/*compile-asserts*
            [`(instrument* '~(qualify-symbol fn-sym))])
        r#)))
