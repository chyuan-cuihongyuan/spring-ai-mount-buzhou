# impl 576 — StartupPhaseTiming（effort #823）

## 切片

- `buzhou-core/src/main/java/.../core/config/StartupPhaseTiming.java` — CopyOnWriteArrayList 步骤表+volatile endMillis+Step 内部类持有 owner clock。
- `buzhou-core/src/test/java/.../core/config/StartupPhaseTimingTest.java` — 5 例。

## 口径

- end 首末：endMillis < 0 才写（幂等）。
- 快照构造时未结束保持 -1（不即时补采样）。

## 验证

mvn -pl buzhou-core -am test -Dtest='StartupPhaseTimingTest' → 5/5 绿；快照再生 1 新公共类型。
