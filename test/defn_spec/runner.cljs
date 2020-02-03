(ns defn-spec.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [cljs.test :as t :include-macros true]
            [defn-spec.core-test]))

(doo-tests 'defn-spec.core-test)
