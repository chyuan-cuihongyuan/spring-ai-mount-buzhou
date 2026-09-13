# impl 562 — JobDeadLetterLog（effort #809）

## 切片

- `buzhou-core/src/main/java/.../core/concurrent/JobDeadLetterLog.java` — 环（ArrayDeque+ReentrantLock）+聚合（LinkedHashMap<String,long[]>+lastErrorByKey）+truncated+snapshot。
- `buzhou-core/src/main/java/.../core/concurrent/DelayedJobQueue.java` — 补丁：failureObserver 字段+2 参构造（旧构造委托 null）+catch 内观察者调用（try/ignore 隔离）。
- `buzhou-core/src/test/java/.../core/concurrent/JobDeadLetterLogTest.java` — 5 例。

## 口径

- 聚合键封顶后明细环仍在记（truncated=true 只影响聚合口径——snapshot 如实）。
- errorType=异常类简名（getSimpleName——跨栈稳定）。

## 验证

mvn -pl buzhou-core -am test -Dtest='JobDeadLetterLogTest,DelayedJobQueueTest' → 8/8 绿；快照再生 1 新公共类型。
