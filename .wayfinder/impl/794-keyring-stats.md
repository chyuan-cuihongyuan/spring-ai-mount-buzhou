# 794 — 签名验钥分布读面

**What to build:** SigningKeyRing verifyAttempts/verifyKeyMisses/rotations 三计数 + 嵌套 KeyRingStats（含 activeVersion/minVerifyVersion 上下文）+ stats() + KeyPairGenerator 测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] verifyAttempts/verifyKeyMisses/rotations 计数埋点
- [x] KeyRingStats 嵌套 record + stats()
- [x] KeyRingStatsTest（轮换/命中/未知版本 miss/低于 min miss/重复轮换拒绝/fresh）
- [x] spec 1042 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-guard test -Dtest='KeyRingStatsTest'` 全绿 + 既有审计签名回归绿。commit 见本轮 `feat(guard)` 提交。
