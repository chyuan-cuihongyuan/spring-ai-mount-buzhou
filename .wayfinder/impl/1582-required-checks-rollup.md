# impl 1582 — 必选检查聚合（spec 2031 / T3163–T3164 / R32）

纵切片：`RequiredChecksRollup`（core/policy 主）+
`RequiredChecksRollupTest`（八用例）。一票否决优先级、挂起语义、
可选显形。

- 测试：`mvn -pl buzhou-core test -Dtest=RequiredChecksRollupTest` 8/8 绿。
