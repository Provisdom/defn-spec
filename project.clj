(defproject defn-spec "0.1.2"
  :description "A Clojure(Script) wrapper around `defn` that optionally adds
                checking to a function's args and/or return value via assertions."
  :url "https://github.com/Provisdom/defn-spec"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1" :scope "provided"]
                 [org.clojure/spec.alpha "0.2.176" :scope "provided"]]
  :deploy-repositories [["releases" "https://clojars.org/repo"]
                        ["snapshots" "https://clojars.org/repo"]]
  :test-selectors {:default    (complement :production)
                   :production :production})
