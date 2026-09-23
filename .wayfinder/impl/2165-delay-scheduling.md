# impl 2165 — S 会话 S15 延迟调度（spec 5014 / T6129–T6130 / S15）

纵切片：DelayScheduling（core/policy）——等待预算 + 降级梯 +
确定性轮次判定。

- 验证：`mvn -pl buzhou-core test -Dtest='DelaySchedulingTest'` 全绿。
