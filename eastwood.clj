(disable-warning
 {:linter :deprecations
  :symbol-matches #{#"^public int java\.util\.logging\.LogRecord\.getThreadID\(\)$"}
  :reason "The replacement, getLongThreadID, was added in JDK16 – but we still support JDK8."})
