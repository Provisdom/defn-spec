# defn-spec

A Clojure(Script) wrapper around `defn` that optionally adds checking to a 
function's args and/or return value via assertions.

NOTE: This is an experimental idea. Feedback (good idea, bad idea, this/that 
doesn't work, etc.) is appreciated.

## Installation

[](dependency)
```clojure
[defn-spec "0.1.0"]
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

## Why not `fdef`?

`fdef` requires that you keep the Specs for your function's args and return value
in a separate location. This approach lets you easily collocate the Specs for your 
function and its definition.

TODO: Add instrumentation downside

## License

Copyright © 2018 Provisdom

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
