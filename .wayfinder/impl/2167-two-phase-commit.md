# impl 2167 — S 会话 S17 Two-Phase Commit 协调器（spec 5016 / T6133–T6134 / S17）

纵切片：TwoPhaseCoordinator（core/transaction）——投票状态机 +
一票否决 + 非法迁移 fail-fast。

- 验证：`mvn -pl buzhou-core test -Dtest='TwoPhaseCoordinatorTest'` 全绿。
