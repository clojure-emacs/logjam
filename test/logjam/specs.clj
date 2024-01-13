(ns logjam.specs
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as string]
   [logjam.appender :as appender]
   [logjam.framework :as framework]
   [logjam.repl :as repl])
  (:import
   (java.util.regex Pattern)))

(s/def :logjam.level/category simple-keyword?)
(s/def :logjam.level/name simple-keyword?)
(s/def :logjam.level/object any?)
(s/def :logjam.level/weight nat-int?)

(s/def :logjam/level
  (s/keys :req-un [:logjam.level/category
                   :logjam.level/name
                   :logjam.level/object
                   :logjam.level/weight]))

(s/def :logjam.filter/end-time pos-int?)

(s/def :logjam.filter/exceptions
  (s/coll-of string? :kind set?))

(s/def :logjam.filter/level simple-keyword?)

(s/def :logjam.filter/loggers
  (s/coll-of string? :kind set?))

(s/def :logjam.filter/loggers-allowlist :logjam.filter/loggers)

(s/def :logjam.filter/loggers-blocklist :logjam.filter/loggers)

(s/def :logjam.filter/pattern string?)
(s/def :logjam.filter/start-time pos-int?)

(s/def :logjam.filter/threads
  (s/coll-of string? :kind set?))

(s/def :logjam/filters
  (s/keys :opt-un [:logjam.filter/end-time
                   :logjam.filter/exceptions
                   :logjam.filter/level
                   :logjam.filter/loggers
                   :logjam.filter/loggers-allowlist
                   :logjam.filter/loggers-blocklist
                   :logjam.filter/pattern
                   :logjam.filter/start-time
                   :logjam.filter/threads]))

(s/def :logjam.pagination/limit (s/and nat-int? #(< ^long % 100)))
(s/def :logjam.pagination/offset (s/and nat-int? #(< ^long % 100)))

(s/def :logjam.event/search
  (s/keys :opt-un [:logjam.pagination/limit
                   :logjam.pagination/offset
                   :logjam/filters]))

(s/def :logjam.framework/add-appender-fn ifn?)
(s/def :logjam.framework/id string?)
(s/def :logjam.framework/javadoc-url string?)
(s/def :logjam.framework/levels (s/coll-of :logjam/level))
(s/def :logjam.framework/log-fn ifn?)
(s/def :logjam.framework/name string?)
(s/def :logjam.framework/remove-appender-fn ifn?)
(s/def :logjam.framework/root-logger string?)
(s/def :logjam.framework/website-url string?)

(s/def :logjam/framework
  (s/keys :req-un [:logjam.framework/add-appender-fn
                   :logjam.framework/id
                   :logjam.framework/javadoc-url
                   :logjam.framework/levels
                   :logjam.framework/log-fn
                   :logjam.framework/name
                   :logjam.framework/remove-appender-fn
                   :logjam.framework/root-logger
                   :logjam.framework/website-url]))

(s/def :logjam.appender/id string?)
(s/def :logjam.appender/levels (s/coll-of :logjam/level))
(s/def :logjam.appender/logger string?)
(s/def :logjam.appender/size pos-int?)
(s/def :logjam.appender/threshold (s/and nat-int? #(< ^long % 100)))

(s/def :logjam.appender/options
  (s/keys :req-un [:logjam.appender/id]
          :opt-un [:logjam.appender/levels
                   :logjam.appender/logger
                   :logjam.appender/size
                   :logjam.appender/threshold]))

(s/def :logjam/appender
  #(instance? clojure.lang.Atom %))

(s/def :logjam.consumer/callback ifn?)
(s/def :logjam.consumer/filter (s/map-of string? any?))
(s/def :logjam.consumer/id string?)

(s/def :logjam/consumer
  (s/keys :req-un [:logjam.consumer/id]
          :opt-un [:logjam.consumer/callback
                   :logjam.consumer/filter]))

(s/def :logjam.event/argument any?)
(s/def :logjam.event/arguments (s/coll-of :logjam.event/argument :kind vector?))
(s/def :logjam.event/id uuid?)
(s/def :logjam.event/level simple-keyword?)
(s/def :logjam.event/logger (s/with-gen string?
                              #(s/gen (s/and string? (complement string/blank?)))))
(s/def :logjam.event/mdc
  (s/with-gen map? ;; relaxed spec for Timbre
    #(s/gen (s/map-of string? string?)))) ;; strict gen for Logback
(s/def :logjam.event/message (s/and string? (complement string/blank?)))
(s/def :logjam.event/thread string?)
(s/def :logjam.event/timestamp pos-int?)

(s/def :logjam/event
  (s/keys :req-un [:logjam.event/arguments
                   :logjam.event/id
                   :logjam.event/level
                   :logjam.event/logger
                   :logjam.event/mdc
                   :logjam.event/message
                   :logjam.event/thread
                   :logjam.event/timestamp]))

;; logjam.framework

(s/fdef framework/appender
  :args (s/cat :framework :logjam/framework
               :appender :logjam.appender/options)
  :ret :logjam/appender)

(s/fdef framework/appenders
  :args (s/cat :framework :logjam/framework)
  :ret (s/coll-of :logjam/appender))

(s/fdef framework/add-appender
  :args (s/cat :framework :logjam/framework
               :appender :logjam.appender/options)
  :ret :logjam/framework)

(s/fdef framework/add-consumer
  :args (s/cat :framework :logjam/framework
               :appender :logjam.appender/options
               :consumer :logjam/consumer)
  :ret :logjam/framework)

(s/fdef framework/clear-appender
  :args (s/cat :framework :logjam/framework
               :appender :logjam.appender/options)
  :ret :logjam/framework)

(s/fdef framework/consumer
  :args (s/cat :framework :logjam/framework
               :appender :logjam.appender/options
               :consumer :logjam/consumer)
  :ret (s/nilable :logjam/consumer))

(s/fdef framework/event
  :args (s/cat :framework :logjam/framework
               :appender :logjam.appender/options
               :id :logjam.event/id)
  :ret (s/nilable :logjam/event))

(s/fdef framework/events
  :args (s/cat :framework :logjam/framework
               :appender :logjam.appender/options)
  :ret (s/coll-of :logjam/event))

(s/fdef framework/log
  :args (s/cat :framework :logjam/framework
               :event map?)
  :ret nil?)

(s/fdef framework/remove-appender
  :args (s/cat :framework :logjam/framework
               :appender :logjam.appender/options)
  :ret :logjam/framework)

(s/fdef framework/remove-consumer
  :args (s/cat :framework :logjam/framework
               :appender :logjam.appender/options
               :consumer :logjam/consumer)
  :ret :logjam/framework)

(s/fdef framework/update-appender
  :args (s/cat :framework :logjam/framework
               :appender :logjam.appender/options)
  :ret :logjam/framework)

(s/fdef framework/resolve-framework
  :args (s/cat :framework-sym qualified-symbol?)
  :ret (s/nilable :logjam/framework))

(s/fdef framework/resolve-frameworks
  :args (s/or :arity-0 (s/cat)
              :arity-1 (s/cat :framework-syms (s/coll-of qualified-symbol?)))
  :ret (s/map-of :logjam.framework/id :logjam/framework))

(s/fdef framework/search-events
  :args (s/cat :framework :logjam/framework
               :appender :logjam.appender/options
               :criteria map?)
  :ret (s/coll-of :logjam/event))

;; logjam.appender

(s/fdef appender/add-consumer
  :args (s/cat :appender :logjam.appender/options
               :consumer :logjam/consumer)
  :ret :logjam.appender/options)

(s/fdef appender/add-event
  :args (s/cat :appender :logjam.appender/options
               :event :logjam/event)
  :ret :logjam.appender/options)

(s/fdef appender/clear
  :args (s/cat :appender :logjam.appender/options)
  :ret :logjam.appender/options)

(s/fdef appender/consumers
  :args (s/cat :appender :logjam.appender/options)
  :ret (s/coll-of :logjam/consumer))

(s/fdef appender/consumer-by-id
  :args (s/cat :appender :logjam.appender/options
               :id :logjam.consumer/id)
  :ret (s/nilable :logjam/consumer))

(s/fdef appender/event
  :args (s/cat :appender :logjam.appender/options
               :id :logjam.event/id)
  :ret (s/nilable :logjam/event))

(s/fdef appender/events
  :args (s/cat :appender :logjam.appender/options)
  :ret (s/coll-of :logjam/event))

(s/fdef appender/make-appender
  :args (s/cat :appender :logjam.appender/options)
  :ret :logjam.appender/options)

(s/fdef appender/remove-consumer
  :args (s/cat :appender :logjam.appender/options
               :consumer :logjam/consumer)
  :ret :logjam.appender/options)

(s/fdef appender/update-appender
  :args (s/cat :appender :logjam.appender/options
               :settings map?)
  :ret :logjam.appender/options)

(s/fdef appender/update-consumer
  :args (s/cat :appender :logjam.appender/options
               :consumer :logjam/consumer)
  :ret :logjam.appender/options)

;; logjam.repl

(s/def :logjam.repl.option/appender
  (s/nilable (s/or :string string? :keyword keyword?)))

(s/def :logjam.repl.option/callback ifn?)

(s/def :logjam.repl.option/consumer
  (s/nilable (s/or :string string? :keyword keyword?)))

(s/def :logjam.repl.option/exceptions
  (s/nilable (s/coll-of string?)))

(s/def :logjam.repl.option/event uuid?)

(s/def :logjam.repl.option/filters
  (s/nilable (s/map-of keyword? any?)))

(s/def :logjam.repl.option/framework
  (s/nilable (s/or :string string? :keyword keyword?)))

(s/def :logjam.repl.option/logger
  (s/nilable string?))

(s/def :logjam.repl.option/loggers
  (s/nilable (s/coll-of string?)))

(s/def :logjam.repl.option/pattern
  (s/nilable (s/or :string string? :regex #(instance? Pattern %))))

(s/def :logjam.repl.option/size
  (s/nilable pos-int?))

(s/def :logjam.repl.option/threads
  (s/nilable (s/coll-of string?)))

(s/def :logjam.repl.option/threshold
  (s/nilable (s/and nat-int? #(<= 0 % 100))))

(s/fdef repl/add-appender
  :args (s/keys* :opt-un [:logjam.repl.option/framework
                          :logjam.repl.option/appender
                          :logjam.repl.option/filters
                          :logjam.repl.option/logger
                          :logjam.repl.option/size
                          :logjam.repl.option/threshold])
  :ret :logjam/appender)

(s/fdef repl/add-consumer
  :args (s/keys* :opt-un [:logjam.repl.option/appender
                          :logjam.repl.option/callback
                          :logjam.repl.option/consumer
                          :logjam.repl.option/filters
                          :logjam.repl.option/framework])
  :ret :logjam/framework)

(s/fdef repl/appender
  :args (s/keys* :opt-un [:logjam.repl.option/framework
                          :logjam.repl.option/appender])
  :ret (s/nilable :logjam/appender))

(s/fdef repl/appenders
  :args (s/keys* :opt-un [:logjam.repl.option/framework])
  :ret (s/coll-of :logjam/appender))

(s/fdef repl/clear-appender
  :args (s/keys* :opt-un [:logjam.repl.option/framework
                          :logjam.repl.option/appender])
  :ret :logjam/framework)

(s/fdef repl/event
  :args (s/keys* :req-un [:logjam.repl.option/event]
                 :opt-un [:logjam.repl.option/appender
                          :logjam.repl.option/framework])
  :ret :logjam/framework)

(s/fdef repl/events
  :args (s/keys* :opt-un [:logjam.repl.option/appender
                          :logjam.repl.option/exceptions
                          :logjam.repl.option/framework
                          :logjam.repl.option/loggers
                          :logjam.repl.option/pattern
                          :logjam.repl.option/threads])
  :ret :logjam/framework)

(s/fdef repl/framework
  :args (s/keys* :opt-un [:logjam.repl.option/framework])
  :ret :logjam/framework)

(s/fdef repl/remove-appender
  :args (s/keys* :opt-un [:logjam.repl.option/framework
                          :logjam.repl.option/appender])
  :ret :logjam/framework)

(s/fdef repl/remove-consumer
  :args (s/keys* :opt-un [:logjam.repl.option/appender
                          :logjam.repl.option/consumer
                          :logjam.repl.option/framework])
  :ret :logjam/framework)

(s/fdef repl/set-appender!
  :args (s/cat :framework (s/or :string string? :keyword keyword?))
  :ret map?)

(s/fdef repl/set-consumer!
  :args (s/cat :consumer (s/or :string string? :keyword keyword?))
  :ret map?)

(s/fdef repl/shutdown
  :args (s/keys* :opt-un [:logjam.repl.option/framework])
  :ret :logjam/framework)

(s/fdef repl/update-appender
  :args (s/keys* :opt-un [:logjam.repl.option/framework
                          :logjam.repl.option/appender
                          :logjam.repl.option/filters
                          :logjam.repl.option/logger
                          :logjam.repl.option/size
                          :logjam.repl.option/threshold])
  :ret :logjam/framework)

(s/fdef repl/update-consumer
  :args (s/keys* :opt-un [:logjam.repl.option/appender
                          :logjam.repl.option/callback
                          :logjam.repl.option/consumer
                          :logjam.repl.option/filters
                          :logjam.repl.option/framework])
  :ret :logjam/framework)
