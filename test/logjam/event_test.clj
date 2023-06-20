(ns logjam.event-test
  (:require [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [logjam.event :as event]
            [logjam.framework :as framework]
            [logjam.test :as test]))

(def frameworks
  (vals (framework/resolve-frameworks)))

(defspec test-search
  (prop/for-all
   [{:keys [levels]} (gen/elements frameworks)
    criteria (s/gen :logjam.event/search)
    events (s/gen (s/coll-of :logjam/event))]
   (every? #(s/valid? :logjam/event %)
           (event/search levels criteria events))))

(defspec test-search-end-time
  (prop/for-all
   [{:keys [levels]} (gen/elements frameworks)
    ^long end-time (s/gen :logjam.filter/end-time)
    events (s/gen (s/coll-of :logjam/event))]
   (every? #(< ^long (:timestamp %) end-time)
           (event/search levels {:filters {:end-time end-time}} events))))

(defspec test-search-exceptions
  (prop/for-all
   [{:keys [levels]} (gen/elements frameworks)
    exceptions (s/gen :logjam.filter/exceptions)
    events (s/gen (s/coll-of :logjam/event))]
   (let [opts {:filters {:exceptions exceptions}}
         events-found (event/search levels opts events)]
     (set/subset? (set (map :exception events-found))
                  (set (map :exception events))))))

(defspec test-search-level
  (prop/for-all
   [[framework events criteria]
    (gen/let [framework (gen/elements frameworks)
              level (gen/one-of [(gen/elements (map :name (:levels framework)))])
              events (gen/vector (test/event-gen framework) 3)]
      [framework events {:filters {:level level}}])]
   (let [level->weight (into {} (map (juxt :name :weight) (:levels framework)))
         ^long min-weight (level->weight (-> criteria :filters :level))]
     (every? #(>= ^long (level->weight (:level %)) min-weight)
             (event/search (:levels framework) criteria events)))))

(defspec test-search-loggers
  (prop/for-all
   [{:keys [levels]} (gen/elements frameworks)
    loggers (s/gen :logjam.filter/loggers)
    events (s/gen (s/coll-of :logjam/event))]
   (let [opts {:filters {:loggers loggers}}
         events-found (event/search levels opts events)]
     (set/subset? (set (map :logger events-found))
                  (set (map :logger events))))))

(defspec test-search-limit
  (prop/for-all
   [{:keys [levels]} (gen/elements frameworks)
    ^long limit (s/gen :logjam.pagination/limit)
    events (s/gen (s/coll-of :logjam/event))]
   (>= limit (count (event/search levels {:limit limit} events)))))

(defspec test-search-offset
  (prop/for-all
   [{:keys [levels]} (gen/elements frameworks)
    offset (s/gen :logjam.pagination/limit)
    events (s/gen (s/coll-of :logjam/event))]
   (= (drop offset events)
      (event/search levels {:offset offset} events))))

(defspec test-search-start-time
  (prop/for-all
   [{:keys [levels]} (gen/elements frameworks)
    ^long start-time (s/gen :logjam.filter/start-time)
    events (s/gen (s/coll-of :logjam/event))]
   (every? #(>= ^long (:timestamp %) start-time)
           (event/search levels {:filters {:start-time start-time}} events))))
