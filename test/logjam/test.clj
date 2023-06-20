(ns logjam.test
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.test.check.generators :as gen]
            [logjam.specs]))

(stest/instrument)

(defn- exception-gen []
  (->> (gen/tuple gen/string-alphanumeric
                  (gen/map gen/keyword gen/any-printable-equatable))
       (gen/fmap (fn [[msg data]] (ex-info msg data)))))

(defn event-gen [framework]
  (->> (gen/tuple (s/gen :logjam/event)
                  (gen/elements (:levels framework))
                  (exception-gen))
       (gen/fmap (fn [[event level exception]]
                   (cond-> (assoc event :level (:name level))
                     (= :error (:category level))
                     (assoc :exception exception))))))
