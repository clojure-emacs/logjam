(ns logjam.framework-test
  (:require
   [clojure.spec.alpha :as s]
   [clojure.test :refer [deftest is testing]]
   [clojure.test.check.generators :as gen]
   [logjam.framework :as framework]
   [logjam.framework.jul :as jul]
   [logjam.framework.logback :as logback]
   [logjam.framework.timbre :as timbre]
   [logjam.specs]
   [taoensso.encore :as encore]))

(def appender
  {:id "my-appender"})

(def frameworks
  [jul/framework logback/framework timbre/framework])

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
      (dotimes [_ 300] ;; b/c generative testing
        (let [timbre? (= "timbre" (:id framework))
              base-event (assoc (gen/generate (s/gen :logjam/event))
                                :level (case (keyword (:id framework))
                                         :timbre :info
                                         :INFO)
                                :logger (:root-logger framework))
              format? (seq (:arguments base-event))
              event (cond-> base-event
                      (and timbre? format?)
                      (assoc :message (apply str "foo" (repeat (count (:arguments base-event))
                                                               " %s"))))
              framework (framework/add-appender framework appender)]
          (is (nil? (framework/log framework event)))
          (let [events (framework/events framework appender)]
            (is (= 1 (count events)))
            (let [captured-event (first events)]
              (when (or (not timbre?)
                        format?)
                (is (= (:arguments event) (:arguments captured-event))))
              (is (uuid? (:id captured-event)))
              (is (= (:level event) (:level captured-event)))
              (when-not timbre? ;; timbre uses ns strings instead of logger ids
                (is (= (:logger event) (:logger captured-event))))
              (is (= (case (keyword (:id framework))
                       :jul {} ;; not supported
                       :log4j2 (:mdc event)
                       :logback (:mdc event)
                       :timbre (:mdc event))
                     (:mdc captured-event)))
              (if format?
                (is (= (:message captured-event) ;; nil
                       (encore/format* (:message event) (:arguments event))))
                (is (= (:message event) (:message captured-event))))
              (is (= (.getName (Thread/currentThread))
                     (:thread captured-event)))
              (is (pos-int? (:timestamp captured-event)))))
          (framework/remove-appender framework appender))))))
