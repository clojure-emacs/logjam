(ns logjam.framework.timbre
  "Log event capturing implementation for Timbre."
  {:added "0.2.0"}
  (:require
   [logjam.appender :as appender]
   [taoensso.timbre :as timbre])
  (:import
   (java.util Date)))

(def ^:private log-levels
  "The Timbre level descriptors."
  (->> [:trace :debug :info :warn :error :fatal :report]
       (map-indexed #(assoc {:name %2
                             :category %2
                             :object %2}
                            :weight %1))))

(defn- extract-event-data [{:keys [vargs level msg_ ^Date instant context ^Throwable ?err thread ?file ^String ?ns-str ?line ?column appender-id]}]
  (cond-> {:arguments vargs ;; `vargs` are the arguments passed to a errorf, infof, etc call
           :id (java.util.UUID/randomUUID)
           :level level
           :logger (or (when (not-empty ?ns-str)
                         (let [sb (StringBuilder. ?ns-str)]
                           (when ?line
                             (.append sb \:)
                             (.append sb (int ?line)))
                           (when ?column
                             (.append sb \:)
                             (.append sb (int ?column)))
                           (.toString sb)))

                       (not-empty ?file)

                       appender-id)
           :message (or (not-empty @msg_)
                        (some-> ?err .getMessage))
           :thread (or thread
                       (.getName (Thread/currentThread)))
           :timestamp (some-> instant .getTime)
           :mdc (or context {})}
    ?err (assoc :exception ?err)))

(defn add-thread-info [data]
  (assoc data :thread (.getName (Thread/currentThread))))

(defn add-thread-info-var? [x]
  (and (var? x)
       (= {:name 'add-thread-info,
           :ns "logjam.framework.timbre"}
          (-> x
              meta
              (select-keys [:name :ns])
              (update :ns str)))))

(defn- add-appender
  "Attach the Timbre appender."
  [framework appender]
  (let [appender-id (:id @appender)
        f {:enabled? true
           :output-opts {:stacktrace-fonts {}}
           :fn (fn [event-data]
                 (swap! appender appender/add-event (extract-event-data event-data)))}]
    (assert appender-id)
    (timbre/merge-config! {:appenders {appender-id f}
                           :middleware (->> timbre/*config*
                                            :middleware
                                            (remove add-thread-info-var?)
                                            (into [#'add-thread-info]))})
    (swap! appender assoc :instance f)
    framework))

(defn- log [_framework {:keys [arguments exception level message mdc]}]
  (timbre/with-context (or mdc {})
    (let [msg+ex (into []
                       (remove nil?)
                       [(not-empty message), exception])
          format? (seq arguments)
          vargs (if format?
                  (into msg+ex arguments)
                  msg+ex)]
      (if format?
        (timbre/log! {:level level :msg-type :f :vargs vargs})
        (timbre/log! {:level level :msg-type :p :vargs vargs})))))

(defn- remove-appender
  "Remove `appender` from the Timbre`framework`."
  [framework appender]
  (let [appender-id (:id @appender)]
    (assert appender-id)
    (timbre/merge-config! {:appenders {appender-id nil}
                           :middleware (->> timbre/*config*
                                            :middleware
                                            (remove add-thread-info-var?)
                                            (into []))}))
  framework)

(def framework
  "Timbre: Pure Clojure/Script logging library."
  {:add-appender-fn #'add-appender
   :id "timbre"
   :javadoc-url "https://github.com/taoensso/timbre"
   :levels log-levels
   :log-fn #'log
   :name "Timbre"
   :remove-appender-fn #'remove-appender
   :root-logger "cider-log-timbre-root-logger"
   :website-url "https://github.com/taoensso/timbre"})
