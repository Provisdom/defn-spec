#!/usr/bin/env bash

clojure -A:test:test-runner --reporter documentation "$@"