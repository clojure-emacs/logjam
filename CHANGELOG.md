# Changelog

## master (unreleased)

## 0.3.0 (2024-03-03)

* [#16](https://github.com/clojure-emacs/logjam/issues/16): handle `java.net.SocketException`s that can be thrown for disconnected clients.
* Introduce `logjam.appender.default-event-size` configuration option.
* [#13](https://github.com/clojure-emacs/logjam/issues/13): Event filters: introduce `:loggers-blocklist`.
  * `:loggers` will remain supported.

## 0.2.0 (2024-01-04)

* [#8](https://github.com/clojure-emacs/logjam/issues/8): Introduce Timbre compatibility.

## 0.1.1 (2023-06-26)

* Initial release
  * `java.util.logging` (Java Logging API, JUL) -compatible
  * Logback-compatible
