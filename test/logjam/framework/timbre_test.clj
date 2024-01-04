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
  (last (framework/events *framework* appender-map)))

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
                (last-event)))))
