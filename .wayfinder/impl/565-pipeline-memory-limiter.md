# impl 565 — PipelineMemoryLimiter（effort #812）

## 切片

- `buzhou-observability/src/main/java/.../observability/PipelineMemoryLimiter.java` — AtomicLong 四计数+tryAdmit CAS 循环+release updateAndGet 防负。
- `buzhou-observability/src/test/java/.../observability/PipelineMemoryLimiterTest.java` — 6 例（含 ExecutorService 8 线程守恒压测）。

## 口径

- 拒收路径只 refused++，不触碰 inFlight（CAS 失败重试不重复计数——循环内每次 current 重读）。

## 验证

mvn -pl buzhou-observability -am test -Dtest='PipelineMemoryLimiterTest' → 6/6 绿；快照再生 1 新公共类型。
