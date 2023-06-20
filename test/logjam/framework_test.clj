(ns logjam.framework-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest is testing]]
            [clojure.test.check.generators :as gen]
            [logjam.framework :as framework]
            [logjam.framework.jul :as jul]
            [logjam.framework.logback :as logback]
            [logjam.specs]))

(def appender
  {:id "my-appender"})

(def frameworks
  [jul/framework logback/framework])

(deftest test-add-appender
  (doseq [framework frameworks]
    (let [framework (framework/add-appender framework appender)]
      (is (framework/appender framework appender))
      (framework/remove-appender framework appender))))

(deftest test-remove-appender
  (doseq [framework frameworks]
    (let [framework (-> (framework/add-appender framework appender)
                        (framework/remove-appender appender))]
      (is (nil? (framework/appender framework appender))))))

(deftest test-log-levels
  (doseq [framework frameworks]
    (testing (:name framework)
      (is (every? #(s/valid? :logjam/level %) (:levels framework))))))

(deftest test-log-message
  (doseq [framework frameworks]
    (testing (:name framework)
      (let [event (assoc (gen/generate (s/gen :logjam/event))
                         :level :INFO
                         :logger (:root-logger framework))
            framework (framework/add-appender framework appender)]
        (is (nil? (framework/log framework event)))
        (let [events (framework/events framework appender)]
          (is (= 1 (count events)))
          (let [captured-event (first events)]
            (is (= (:arguments event) (:arguments captured-event)))
            (is (uuid? (:id captured-event)))
            (is (= (:level event) (:level captured-event)))
            (is (= (:logger event) (:logger captured-event)))
            (is (= (case (keyword (:id framework))
                     :jul {} ;; not supported
                     :log4j2 (:mdc event)
                     :logback (:mdc event))
                   (:mdc captured-event)))
            (is (= (:message event) (:message captured-event)))
            (is (= (.getName (Thread/currentThread))
                   (:thread captured-event)))
            (is (pos-int? (:timestamp captured-event)))))
        (framework/remove-appender framework appender)))))
