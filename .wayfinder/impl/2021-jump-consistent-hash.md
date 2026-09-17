# impl 2021 — Q 会话 R21 跳增一致性哈希（spec 3020 / T5041–T5042 / R21）

纵切片：JumpConsistentHash（core/policy）——跳增桶映射 + 最小迁移
判定 + 契约边界声明。

- 验证：`mvn -pl buzhou-core test -Dtest='JumpConsistentHashTest'` 全绿。
