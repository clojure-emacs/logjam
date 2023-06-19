;; PROJECT_VERSION is set by .circleci/deploy/deploy_release.clj,
;; whenever we perform a deployment.
(defproject mx.cider/logjam (or (not-empty (System/getenv "PROJECT_VERSION"))
                                "0.0.0")
  :description ""
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
  :profiles {:provided {:dependencies [[org.clojure/clojure "1.11.1"]]}

             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}

             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}

             :1.10 {:dependencies [[org.clojure/clojure "1.10.3"]]}

             :1.11 {:dependencies [[org.clojure/clojure "1.11.1"]]}

             :master {:repositories [["snapshots"
                                      "https://oss.sonatype.org/content/repositories/snapshots"]]
                      :dependencies [[org.clojure/clojure "1.12.0-master-SNAPSHOT"]
                                     [org.clojure/clojure "1.12.0-master-SNAPSHOT" :classifier "sources"]]}

             :cljfmt {:plugins [[lein-cljfmt "0.9.2" :exclusions [org.clojure/clojure
                                                                  org.clojure/clojurescript]]]}
             :eastwood {:plugins         [[jonase/eastwood "1.4.0"]]
                        :eastwood {:add-linters [:performance :boxed-math]}}
             :clj-kondo {:dependencies [[clj-kondo "2023.05.26"]]}
             :deploy {:source-paths [".circleci/deploy"]}})
