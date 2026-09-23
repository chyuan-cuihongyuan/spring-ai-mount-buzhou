# impl 2134 — R 会话 R34 语义化版本序（spec 4033 / T6067–T6068 / R34）

纵切片：SemVerOrder（core/policy）——parse 全量校验 + §11
优先级比较 + newerThan 读数。

- 验证：`mvn -pl buzhou-core test -Dtest='SemVerOrderTest'` 全绿。
