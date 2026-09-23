# impl 2169 — S 会话 S19 Read Repair 读修复（spec 5017a / T6137–T6138 / S19）

纵切片：ReadRepair（core/transaction）——版本择优 + 陈旧清单
+ tie-break + fail-fast。

- 验证：`mvn -pl buzhou-core test -Dtest='ReadRepairTest'` 全绿。
