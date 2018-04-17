(defproject defn-spec "0.1.1"
  :description "A Clojure(Script) wrapper around `defn` that optionally adds
                checking to a function's args and/or return value via assertions."
  :url "https://github.com/Provisdom/defn-spec"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/spec.alpha "0.1.143"]]
  :deploy-repositories [["releases" "https://clojars.org/repo"]
                        ["snapshots" "https://clojars.org/repo"]])
