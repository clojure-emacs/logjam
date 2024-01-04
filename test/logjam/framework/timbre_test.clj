(ns logjam.framework.timbre-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [logjam.framework :as framework]
   [logjam.framework.timbre :as sut]
   [taoensso.timbre :as timbre]))

;; for `match?`
(require 'matcher-combinators.test)

(def appender-map {:id (str `timbre)})

(def ^:dynamic *framework* nil)

(use-fixtures :each (fn with-timbre-framework [t]
                      (binding [*framework* (framework/add-appender sut/framework appender-map)]
                        (try
                          (t)
                          (finally
                            (framework/remove-appender *framework* appender-map))))))

(defn last-event []
  (first (framework/events *framework* appender-map)))

(deftest timbre-test
  (testing "Basic case"
    (timbre/info "sample")
    (is (match? {:arguments ["sample"],
                 :mdc {},
                 :level :info,
                 :thread (.getName (Thread/currentThread))
                 :id uuid?,
                 :logger "logjam.framework.timbre-test:27:5",
                 :timestamp nat-int?,
                 :message "sample"}
                (last-event))))

  (testing "The thread name is accurately captured"
    (let [thread-name (str "a" (java.util.UUID/randomUUID))]
      (-> #(timbre/info "sample")
          Thread.
          (doto (.setName thread-name))
          (doto .start)
          .join)
      (is (match? {:thread thread-name}
                  (last-event)))))

  (testing "Exceptions' messages can serve as the log message"
    (timbre/info (ex-info "the error message" {}))
    (is (match? {:message "the error message"}
                (last-event))))

  (testing "Exceptions are captured"
    (let [ex (ex-info "the error message" {})]
      (timbre/info ex)
      (is (= ex
             (:exception (last-event))))))

  (testing "Timbre context is recorded as `:mdc`"
    (timbre/with-context {:a 1 :b 2}
      (timbre/info "foo"))
    (is (match? {:mdc {:a 1 :b 2}}
                (last-event))))

  (testing "Formatted logging"
    (timbre/infof "foo %s" 42)

    (is (match? {:message "foo 42"}
                (last-event))
        "The captured `:message` is the application of the arguments over the template")

    (is (match? {:arguments [42]}
                (last-event))
        "The arguments are accurately captured")))
