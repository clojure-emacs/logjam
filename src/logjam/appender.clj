(ns logjam.appender
  "A log appender that captures log events in memory."
  {:author "r0man"}
  (:require [logjam.event :as event]))

(def ^:private default-size
  "The default maximum number of events that can be captured by a given appender."
  (or (some-> (System/getProperty "logjam.appender.default-event-size") Long/parseLong)
      100000))

(def ^:private default-threshold
  "The default threshold in percentage after which log events are cleaned up.

  Events of a log appender are cleanup up if the number of events reach the
  `default-size` plus the `default-threshold` percentage of
  `default-threshold`."
  10)

(defn- garbage-collect?
  "Whether to garbage collect events, or not."
  [{:keys [event-index ^long size ^long threshold]}]
  (> (count event-index)
     (+ size (* size (/ threshold 100.0)))))

(defn- garbage-collect-events
  "Garbage collect some events of the `appender`."
  [{:keys [events event-index size] :as appender}]
  (if (garbage-collect? appender)
    (assoc appender
           :events (take size events)
           :event-index (apply dissoc event-index (map :id (drop size events))))
    appender))

(defn- add-event?
  "Whether the `event` should be added to the appender."
  [{:keys [filter-fn]} event]
  (or (nil? filter-fn) (filter-fn event)))

(defn- notify-consumers
  "Calls the `:callback` present in each consumer.

  If a SocketException is found, remove the consumer,
  as it represents a disconnected nREPL client."
  [{:keys [consumers] :as appender} event]
  (doseq [[consumer-id {:keys [callback filter-fn] :as consumer}] (some-> consumers deref)
          :when (filter-fn event)]
    (try
      (callback consumer event)
      (catch java.net.SocketException e ;; Issue #16
        (.printStackTrace e)
        (swap! consumers dissoc consumer-id))))
  appender)

(defn- enqueue-event
  "Enqueue the `event` to the event list of `appender`."
  [appender event]
  (update appender :events #(cons event %)))

(defn- index-event
  "Add the `event` to the index of `appender`."
  [appender event]
  (assoc-in appender [:event-index (:id event)] event))

(defn add-consumer
  "Add the `consumer` to the `appender`."
  [appender {:keys [id filters] :as consumer}]
  (when-let [consumers (some-> appender (get :consumers) deref)]
    (assert (not (get consumers id))
            (format "Consumer %s already registered" id)))
  (swap! (:consumers appender)
         assoc
         id
         (-> (select-keys consumer [:callback :filters :id])
             (assoc :filter-fn (event/search-filter (:levels appender) filters))))
  appender)

(defn add-event
  "Add the `event` to the `appender`."
  [appender event]
  (if (add-event? appender event)
    (-> (enqueue-event appender event)
        (index-event event)
        (notify-consumers event)
        (garbage-collect-events))
    appender))

(defn clear
  "Clear the events from the `appender`."
  [appender]
  (assoc appender :events [] :event-index {}))

(defn consumers
  "Return the consumers of the `appender`."
  [appender]
  (some-> appender :consumers deref vals))

(defn consumer-by-id
  "Find the consumer of `appender` by `id`."
  [appender id]
  (some #(and (= id (:id %)) %) (consumers appender)))

(defn event
  "Lookup the event by `id` from the log `appender`."
  [appender id]
  (get (:event-index appender) id))

(defn events
  "Return the events from the `appender`."
  [appender]
  (take (:size appender) (:events appender)))

(defn make-appender
  "Make a hash map appender."
  [{:keys [id filters levels logger size threshold]}]
  (cond-> {:consumers (atom {})
           :event-index {}
           :events nil
           :filters (or filters {})
           :id id
           :levels levels
           :size (or size default-size)
           :threshold (or threshold default-threshold)}
    (map? filters)
    (assoc :filter-fn (event/search-filter levels filters))
    logger
    (assoc :logger logger)))

(defn remove-consumer
  "Remove the `consumer` from the `appender`."
  [appender consumer]
  (some-> appender :consumers (swap! dissoc (:id consumer)))
  appender)

(defn update-appender
  "Update the log `appender`."
  [appender {:keys [filters size threshold]}]
  (cond-> appender
    (map? filters)
    (assoc :filters filters :filter-fn (event/search-filter (:levels appender) filters))
    (pos-int? size)
    (assoc :size size)
    (nat-int? threshold)
    (assoc :threshold threshold)))

(defn update-consumer
  "Update the `consumer` of the `appender`."
  [appender {:keys [id filters] :as consumer}]
  (some-> appender
          :consumers
          (swap! update
                 id
                 (fn [existing-consumer]
                   (assert (:id existing-consumer)
                           (format "Consumer %s not registered" id))
                   (-> existing-consumer
                       (merge (select-keys consumer [:filters]))
                       (assoc :filter-fn (event/search-filter (:levels appender) filters))))))
  appender)
