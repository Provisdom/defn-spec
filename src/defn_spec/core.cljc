(ns defn-spec.core
  (:require
    [clojure.spec.alpha :as s]
    [defn-spec.defn-args :as defn-args]
    [clojure.spec.test.alpha :as st]))

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
       (let [{:keys [name docstring meta bs]} (s/conform ::defn-args/defn-args args)
             qualified-name (qualify-symbol name)
             args-spec (or (::s/args meta) (:cljs.spec.alpha/args meta))
             ret-spec (or (::s/ret meta) (:cljs.spec.alpha/ret meta))
             args-sym (gensym "args")
             result-sym (gensym "result")]
         `(let [var# (defn ~@args)
                initial-fn# (var-get var#)]
            (alter-var-root
             var#
             (constantly
              (fn
                [& ~args-sym]
                ~@(when args-spec [`(assert* :args '~qualified-name ~args-spec ~args-sym)])
                (let [~result-sym (apply initial-fn# ~args-sym)]
                  ~@(when ret-spec [`(assert* :ret '~qualified-name (s/spec ~ret-spec) ~result-sym)])
                  ~result-sym))))
            var#))
       `(defn ~@args))))

#?(:clj
   (s/fdef defn-spec
           :args ::defn-args/defn-args
           :ret any?))

#?(:clj
   (defmacro fdef
     "Exact same parameters as `s/fdef`. Automatically enables instrumentation
     for `fn-sym` when `s/*compile-asserts*` is true."
     [fn-sym & specs]
     `(let [r# (s/fdef ~fn-sym ~@specs)]
        ~@(when s/*compile-asserts*
            [`(st/instrument '~(qualify-symbol fn-sym))])
        r#)))
