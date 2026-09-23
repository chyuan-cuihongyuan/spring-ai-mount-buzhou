# impl 2129 — R 会话 R29 动态 snitch 惩罚（spec 4028 / T6057–T6058 / R29）

纵切片：DynamicSnitchPenalty（core/policy）——EWMA + 罚分 + 排序
推尾 + 恢复复用。

- 验证：`mvn -pl buzhou-core test -Dtest='DynamicSnitchPenaltyTest'` 全绿。
