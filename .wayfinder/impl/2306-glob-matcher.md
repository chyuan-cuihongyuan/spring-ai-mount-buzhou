# impl 2306 — V 会话 V5 GlobMatcher（spec 8004 / V8009–V8010 / V5）

纵切片：GlobMatcher（core/metrics）——fnmatch 四原语 token 化+星号回溯线性匹配。

- 验证：`mvn -pl buzhou-core test -Dtest='GlobMatcherTest'` 三测全绿。
