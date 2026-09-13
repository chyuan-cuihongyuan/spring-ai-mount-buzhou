# impl 588 — TailSamplingDecisionLog（effort #835）

## 切片

- `buzhou-observability/src/main/java/.../observability/TailSamplingDecisionLog.java` — 环+LinkedHashMap 决策×原因键+synchronized(lock)。
- `buzhou-observability/src/test/java/.../observability/TailSamplingDecisionLogTest.java` — 4 例。

## 口径

- 聚合键=决策名+\0+原因（溢出桶也带决策维——KEPT/DROPPED 分账）。
- 溢出桶计入 truncated 标记。

## 验证

mvn -pl buzhou-observability -am test -Dtest='TailSamplingDecisionLogTest' → 4/4 绿；快照再生 1 新公共类型。
