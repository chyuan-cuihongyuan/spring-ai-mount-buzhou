# impl-124 — 审计轮换持久化与外锚

**What to build:** 运行期轮换落盘（写而后切）重启自动入环；校验报告链头哈希 + 外锚比对。

**Blocked by:** None

**Status:** done

- [x] SigningKeyPersister + PemFileKeyPersister（v<version>.pem 约定命名原子落盘）
- [x] SigningKeyRing 写而后切（失败中止）；PemFileKeyProvider.scanDirectory
- [x] GuardAuditConfig.signing.key-dir + auto-config 接线（扫描 + 持久化器同挂）
- [x] VerificationReport.headHash/anchorMatched + verify 三参重载 + anchored()
- [x] 测试：轮换落盘重启入环/失败中止/锚点一致/删尾检出——guard 96 绿
