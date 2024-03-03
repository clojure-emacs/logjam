.PHONY: test kondo eastwood cljfmt lint deploy clean repl lein-repl
VERSION ?= 1.11
HOME=$(shell echo $$HOME)
HERE=$(shell echo $$PWD)

# This makefile offers high-performance (cached) wrappers that enrich the classpath, for optimal IDE functioning.
# We suggest that you copy/merge this file into your project.
# We believe it's best to give you something that you can freely understand and hack locally.
# The tradeoff is that you may have to catch up with important fixes/improvements to this Makefile, if any.
# Feel free to suggest any improvements at:
# https://github.com/clojure-emacs/enrich-classpath/issues

# Set bash instead of sh for the @if [[ conditions,
# and use the usual safety flags:
SHELL = /bin/bash -Eeu

# The Lein profiles that will be selected for `lein-repl`.
# Feel free to upgrade this, or to override it with an env var named LEIN_PROFILES.
# Expected format: "+dev,+test"
# Don't use spaces here.
LEIN_PROFILES ?= "+dev,+test"

# The enrich-classpath version to be injected.
# Feel free to upgrade this.
ENRICH_CLASSPATH_VERSION="1.19.0"

# Create and cache a `java` command. project.clj is mandatory; the others are optional but are taken into account for cache recomputation.
# It's important not to silence with step with @ syntax, so that Enrich progress can be seen as it resolves dependencies.
.enrich-classpath-lein-repl: Makefile project.clj $(wildcard checkouts/*/project.clj) $(wildcard deps.edn) $(wildcard $(HOME)/.clojure/deps.edn) $(wildcard profiles.clj) $(wildcard $(HOME)/.lein/profiles.clj) $(wildcard $(HOME)/.lein/profiles.d) $(wildcard /etc/leiningen/profiles.clj)
	bash 'lein' 'update-in' ':plugins' 'conj' "[mx.cider/lein-enrich-classpath \"$(ENRICH_CLASSPATH_VERSION)\"]" '--' 'with-profile' $(LEIN_PROFILES) 'update-in' ':middleware' 'conj' 'cider.enrich-classpath.plugin-v2/middleware' '--' 'repl' | grep " -cp " > $@

# Launches a repl, falling back to vanilla lein repl if something went wrong during classpath calculation.
lein-repl: .enrich-classpath-lein-repl
	@if grep --silent " -cp " .enrich-classpath-lein-repl; then \
		eval "$$(cat .enrich-classpath-lein-repl) --interactive"; \
	else \
		echo "Falling back to lein repl... (you can avoid further falling back by removing .enrich-classpath-lein-repl)"; \
		lein with-profiles $(LEIN_PROFILES) repl; \
	fi

clean:
	lein clean

test: clean
	lein with-profile -user,-dev,+$(VERSION) test

cljfmt:
	lein with-profile -user,+$(VERSION),+test,+cljfmt cljfmt check

cljfmt-fix:
	lein with-profile -user,+$(VERSION),+test,+cljfmt cljfmt fix

eastwood:
	lein with-profile -user,-dev,+$(VERSION),+test,+deploy,+eastwood eastwood

.make_kondo_prep: project.clj .clj-kondo/config.edn
	lein with-profile -dev,+$(VERSION),+test,+clj-kondo,+deploy clj-kondo --copy-configs --dependencies --parallel --lint '$$classpath' > $@

kondo: .make_kondo_prep clean
	lein with-profile -dev,+$(VERSION),+test,+clj-kondo,+deploy clj-kondo

repl: lein-repl

lint: kondo cljfmt eastwood

# Deployment is performed via CI by creating a git tag prefixed with "v".
# Please do not deploy locally as it skips various measures.
deploy: check-env
	lein with-profile -user,-dev,+$(VERSION) deploy clojars

# Usage: PROJECT_VERSION=0.3.0 make install
# PROJECT_VERSION is needed because it's not computed dynamically.
install: check-install-env
	lein with-profile -user,-dev,+$(VERSION) install

check-env:
ifndef CLOJARS_USERNAME
	$(error CLOJARS_USERNAME is undefined)
endif
ifndef CLOJARS_PASSWORD
	$(error CLOJARS_PASSWORD is undefined)
endif
ifndef CIRCLE_TAG
	$(error CIRCLE_TAG is undefined)
endif

check-install-env:
ifndef PROJECT_VERSION
	$(error Please set PROJECT_VERSION as an env var beforehand.)
endif
