# impl 564 — CircuitCrashLoopDetector（effort #811）

## 切片

- `buzhou-resilience/src/main/java/.../resilience/CircuitCrashLoopDetector.java` — ModelState（Deque+volatile looping+loopsDetected）+MAX_MODELS 32+滑窗判定（synchronized per-state）。
- `buzhou-resilience/src/test/java/.../resilience/CircuitCrashLoopDetectorTest.java` — 6 例。

## 口径

- loopsDetected 在 !looping→looping 边沿 +1（进入计数非持续计数）。
- recordRecovery 清 deque+looping（loopsDetected 保留——历史轮次口径）。

## 验证

mvn -pl buzhou-resilience -am test -Dtest='CircuitCrashLoopDetectorTest' → 6/6 绿；快照再生 1 新公共类型。
