;; PROJECT_VERSION is set by .circleci/deploy/deploy_release.clj,
;; whenever we perform a deployment.
(defproject mx.cider/logjam (or (not-empty (System/getenv "PROJECT_VERSION"))
                                "0.0.0")
  :description "An interactive, nrepl-oriented logging backend"
  :url "https://github.com/clojure-emacs/logjam"
  :license {:name "Eclipse Public License"
            :url "https://www.eclipse.org/legal/epl-v10.html"}
  :dependencies []
  :pedantic? ~(if (System/getenv "CI")
                :abort
                ;; :pedantic? can be problematic for certain local dev workflows:
                false)
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/clojars_username
                                    :password :env/clojars_password
                                    :sign-releases false}]]
  :profiles {:provided {:dependencies [;; 1.3.7 and 1.4.7 are working, but we need 1.3.7 for JDK8
                                       [ch.qos.logback/logback-classic "1.3.7"]
                                       [com.taoensso/timbre "6.3.1" :exclusions [org.clojure/clojure]]
                                       [org.clojure/clojure "1.11.1"]]}

             :dev {:plugins [[cider/cider-nrepl "0.44.0"]
                             [refactor-nrepl "3.9.0"]]}

             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}

             :1.10 {:dependencies [[org.clojure/clojure "1.10.3"]]}

             :1.11 {:dependencies [[org.clojure/clojure "1.11.1"]]}

             :master {:repositories [["snapshots"
                                      "https://oss.sonatype.org/content/repositories/snapshots"]]
                      :dependencies [[org.clojure/clojure "1.12.0-master-SNAPSHOT"]
                                     [org.clojure/clojure "1.12.0-master-SNAPSHOT" :classifier "sources"]]}

             :test {:jvm-opts ["-Djava.util.logging.config.file=test/resources/logging.properties"]
                    :resource-paths ["test/resources"]
                    :dependencies [[nubank/matcher-combinators "3.8.8"]
                                   [org.clojure/test.check "1.1.1" :exclusions [org.clojure/clojure]]]}

             :cljfmt {:plugins [[lein-cljfmt "0.9.2" :exclusions [org.clojure/clojure
                                                                  org.clojure/clojurescript]]]}
             :eastwood {:plugins         [[jonase/eastwood "1.4.2"]]
                        :eastwood {:add-linters [:performance :boxed-math]
                                   :config-files ["eastwood.clj"]}}
             :clj-kondo {:plugins [[com.github.clj-kondo/lein-clj-kondo "2023.10.20"]]
                         :dependencies [[com.fasterxml.jackson.core/jackson-core "2.14.2"]]}
             :deploy {:source-paths [".circleci/deploy"]}})
