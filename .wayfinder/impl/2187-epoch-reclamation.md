# impl 2187 — S 会话 S37 Epoch-Based Reclamation 时代回收（spec 5036 / T6173–T6174 / S37）

纵切片：EpochReclamation（core/concurrent）——守卫钉时代 +
退休记账 + 安全回收判定（时代升序+退休序确定性）。

- 验证：`mvn -pl buzhou-core test -Dtest='EpochReclamationTest'` 全绿。
