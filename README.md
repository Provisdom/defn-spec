# defn-spec

[![CircleCI](https://circleci.com/gh/Provisdom/defn-spec.svg?style=svg)](https://circleci.com/gh/Provisdom/defn-spec)

A Clojure(Script) wrapper around `defn` that optionally adds checking to a 
function's args and/or return value via assertions.

NOTE: This is an experimental idea. Feedback (good idea, bad idea, this/that 
doesn't work, etc.) is appreciated.

## Installation

[](dependency)
```clojure
[defn-spec "0.1.1"]
```
[](/dependency)


## Usage

You use `defn-spec` in the same way you would use `defn`. 

```clojure
(require '[clojure.spec.alpha :as s])
(require '[defn-spec.core :as ds])

(ds/defn-spec my-inc
  [x]
  (inc x))
```

Now let's add Spec assertions to the above function definition by adding an
attribute map.

```clojure
(ds/defn-spec my-inc
  {::s/args (s/cat :x int?)
   ::s/ret  nat-int?}
  [x]
  (inc x))
```

Open a REPL and try calling your new function...

```clojure
(my-inc 1)
=> 2

(my-inc "a")
ExceptionInfo Call to user/my-inc did not conform to spec:
In: [0] val: "a" fails at: [:x] predicate: int?
  
(my-inc -2)
ExceptionInfo Return value from user/my-inc did not conform to spec:
val: -1 fails predicate: nat-int?
```

If `defn-spec` is not working, ensure `s/*compile-asserts*` is true.

I suggest checking out [expound](https://github.com/bhb/expound) to greatly improve
the above error messages.

### `fdef` automatic instrumentation

If you prefer to keep your function's spec separate from its definition and still
want `instrument` to be enabled automatically, take a gander at `defn-spec.core/fdef`.

```clojure
(defn instrumented-fn
  [x]
  (inc x))

(ds/fdef instrumented-fn
         :args (s/cat :x number?)
         :ret number?)
```

`ds/fdef` works exactly the same as `clojure.spec.alpha/fdef`, differencing itself
by automatically enabling instrumentation for the passed in symbol. Here is a 
cleaned up version of what the macro expands to.

```clojure
(let
  [r (s/fdef instrumented-fn
             :args (s/cat :x number?)
             :ret number?)]
  (st/instrument `instrumented-fn)
  r)
```

If you set `s/*compile-assers*` to `false`, `fdef` will elide the call to `instrument`.

Keeping your `fdef`'s in a separate namespace from where your function is defined
results in you falling susceptible to the [initial/global call issue](#why-not-fdef).
_You Have Been Warned_.

### In Production

Set `s/*compile-asserts*` to `false` for all `defn-spec` expansions to compile to
a regular `defn` form.

```clojure
(binding [s/*compile-asserts* false]
  (macroexpand-1 '(ds/defn-spec my-inc
                    {::s/args (s/cat :x int?)
                     ::s/ret  nat-int?}
                    [x]
                    (inc x))))
=> (clojure.core/defn my-inc 
     #:clojure.spec.alpha{:args (s/cat :x int?), :ret nat-int?} 
     [x] 
     (inc x))
```

## FAQs

### Why not `fdef`?

`fdef` requires that you keep the Specs for your function's args and return value
in a separate location. This approach lets you easily collocate the Specs for your 
function and its definition.

It is not easy to instrument initial/global function calls. This is best explained
by example. Imagine you have an API that has a `reg-event` function. This function 
takes a keyword ID and a handler function and does some side effecting things 
(i.e. registers it in a global registrar). Here is our API namespace: 

```clojure
(ns my-project.api
  (:require
    [clojure.spec.alpha :as s]))

(defn reg-event
  [id handler]
  ;; do side effecting stuff
  nil)

(s/fdef reg-event
        :args (s/cat :id keyword? :handler fn?)
        :ret nil?)

(reg-event "my-event")
```

You'll notice we call the `reg-event` function within the api namespace, but our
call to `reg-event` was incorrect. We do not get a Spec instrumentation error, 
however. This is confusing because you know that you enabled instrumentation
in your `dev.user` namespace.

```clojure
(ns dev.user
  (:require
    [clojure.spec.test.alpha :as st]
    [my-project.api]))

(st/instrument)
```

Thinking carefully about the order things are executed, we discover the problem.

1. Load `dev.user` in the REPL...
2. Require `clojure.spec.test.alpha`.
3. Require `my-project.api`.
4. Run `(st/instrument)`
5. ... `dev.user` finished loading.

The call to `instrument` happens **after** we `require`'ed `my-project.api`.
The incorrect call to `reg-event` in the `my-project.api` namespace was not 
instrumented when it was executed so we do not get an instrumentation error.

You may think that you can simply include the `instrument` call after every `fdef`
but that isn't a great idea for a couple reasons. First, it does not scale well.
You'll need to remember to include the call directly after every `fdef` for every
one of your globally called functions. Let's hope you don't forget. Second, you
need a way to ensure the `instrument` call does not occur in production. Maybe
you wrap `instrument` in a macro to elide its call in production. 

That seems like an awful lot of work to get instrumentation to behave correctly 
for initial/global function calls. 

### How is this different from Orchestra?

[Orchestra](https://github.com/jeaye/orchestra) provides a macro called 
[defn-spec](https://github.com/jeaye/orchestra#defn-spec). This macro (and Orchestra
itself) aims to extend Spec's instrumentation functionality, not take the assertion
approach as this library does. Further, Orchestra's `defn-spec` macro specifies
a new DSL for specifying a function's `:args` and `:ret` specs, which does not 
follow Clojure's `defn` format. IDEs (like Cursive) do not have support for this
new DSL.

## License

Copyright © 2018 Provisdom

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
