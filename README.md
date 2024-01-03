[![CircleCI](https://circleci.com/gh/clojure-emacs/logjam/tree/master.svg?style=svg)](https://circleci.com/gh/clojure-emacs/logjam/tree/master)
[![Clojars](https://img.shields.io/clojars/v/mx.cider/logjam.svg)](https://clojars.org/mx.cider/logjam)
[![Dependabot Status](https://versions.deps.co/clojure-emacs/logjam/status.svg)](https://versions.deps.co/clojure-emacs/logjam)
[![Codecov](https://codecov.io/gh/clojure-emacs/logjam/branch/master/graph/badge.svg)](https://codecov.io/gh/clojure-emacs/logjam/)
[![cljdoc](https://cljdoc.org/badge/mx.cider/logjam)](https://cljdoc.org/d/mx.cider/logjam/CURRENT)
[![Downloads](https://versions.deps.co/mx.cider/logjam/downloads.svg)](https://clojars.org/mx.cider/logjam)

# Logjam

Logjam is a library to capture log events emitted by Java logging
frameworks. Log events are captured as a Clojure data structure in an
in-memory atom, which can be searched, inspected and listened to.

Logjam is designed to be used by tooling such as [CIDER](https://cider.mx/)'s [Log
Mode](https://docs.cider.mx/cider/debugging/logging.html), but can also be used with a plain REPL in combination with
[REBL](https://docs.datomic.com/cloud/other-tools/REBL.html) or [Morse](https://github.com/nubank/morse).

## Overview

### Status

This library is still experimental and APIs are subject to change.

### Log Framework

Logjam supports log frameworks that allow reconfiguration at run
time. More specifically the framework should support attaching log
appenders to loggers, in order to capture events.

At the moment the following log frameworks are supported:

- [Java Util Logging](https://docs.oracle.com/en/java/javase/19/core/java-logging-overview.html)
- [Logback](https://logback.qos.ch)
- [Timbre](https://github.com/taoensso/timbre)

### Log Appender

In order to capture log events, a log appender needs to be attached to
a logger of a framework. Once an appender is attached to a logger it
captures the log events emitted by the framework in an in-memory
atom. A log appender can be configured to have a certain size
(default: 100000) and a threshold in percentage (default: 10). Log
events are cleared from the appender when threshold (appender size
plus threshold in percentage of the appender size) is
reached. Additionally an appender can be configured to only capture
events that match a set of filters.

### Log Consumer

Log events can be streamed to a client by attaching a log consumer to
an appender. Once a log consumer has been attached to an appender, it
will receive events from the appender. Similar to log appenders,
consumers can also be configured with a set of filters to only receive
certain events.

### Log Events

Log events can be searched, streamed to a client or viewed in CIDER's
Inspector and Stacktrace Mode. When searching log events the user can
specify a set of filters. Events that match the filters are shown in
the `+*cider-log*+` buffer. Additionally a log consumer will be
attached to the appender to receive log events matching the search
criteria after the search command has been issued. The log appender
will be removed automatically once a new search has been submitted or
when the `*cider-log*` buffer gets killed.

### Log Filters

Filters for log events can be attached to log appenders and
consumers. They also take effect when searching events or streaming
them to clients. If multiple filters are chosen they are combined
using logical AND condition. The following filters are available:

## Usage

Logjam is used by [CIDER](https://cider.mx/)'s [LogMode](https://docs.cider.mx/cider/debugging/logging.html), but can also be used in a
standalone REPL. Here is an example how such a REPL session could look
like:

``` clojure
;; Use the :logback logging framework
(repl/set-framework! :logback)

;; Add an appender to the log framework that captures events.
(repl/add-appender)

;; Log something
(repl/log :message \"hello\")

;; Return all captured log events
(repl/events)

;; Search log events with :message field matching a regex :pattern
(repl/events :pattern \"hel.*\")

;; Search log events by level
(repl/events :level :INFO)

;; Add a log consumer that prints log events
(repl/add-consumer
  :callback (fn [_consumer event]
              (clojure.pprint/pprint event)))

;; Log something else
(repl/log :message \"world\")

;; Remove all consumers and appenders
(repl/shutdown)
```

## Development

#### Makefile

The Makefile offers a variety of tasks: `test kondo eastwood cljfmt lint clean repl`.

#### Deployment

Here's how to deploy to Clojars:

```bash
git tag -a v0.1.0 -m "0.1.0"
git push --tags
```

## [Changelog](CHANGELOG.md)

## Thanks

Thanks to @rafaeldff for his initial idea on capturing the logs and
supporting this project. Some of the code in this repository is based
on his private development tooling.

## License

Copyright © 2023-2024 CIDER Contributors

Distributed under the Eclipse Public License, the same as Clojure.
