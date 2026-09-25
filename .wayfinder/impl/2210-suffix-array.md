# impl 2210 — T 会话 T10 Suffix Array 后缀数组（spec 6009 / T6219–T6220 / T10）

纵切片：SuffixArray（core/metrics）——倍增构造+Kasai LCP+
二分 contains/occurrenceCount（初版 contains 以 ≥ 代替 =，
banana 例钉住修正为等值判定）。

- 验证：`mvn -pl buzhou-core test -Dtest='SuffixArrayTest'` 全绿（MVN_EXIT=0）。
