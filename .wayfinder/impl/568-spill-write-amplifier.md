# impl 568 — SpillWriteAmplifier（effort #815）

## 切片

- `buzhou-spill/src/main/java/.../spill/SpillWriteAmplifier.java` — ReentrantLock 计数+Deque<Sample> 近窗+最近秩 P95。
- `buzhou-spill/src/test/java/.../spill/SpillWriteAmplifierTest.java` — 5 例。

## 口径

- recentRatio=近窗均值、recentP95Ratio=升序 ⌈0.95n⌉ 位（1-based）。
- recordWrite 在锁内更新 total+窗（单锁双账——一致性优先于吞吐）。

## 验证

mvn -pl buzhou-spill -am test -Dtest='SpillWriteAmplifierTest' → 5/5 绿；快照再生 1 新公共类型。
