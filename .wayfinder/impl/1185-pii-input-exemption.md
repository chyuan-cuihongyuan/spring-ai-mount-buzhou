# 1185 — 输入侧 PII 豁免 + J 系测试解卡

**What to build:** PiiInputRedactionHook +exemptions 双粒度 + DangerousToolStatsTest 三处解卡。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] PiiInputRedactionHook：4 参构造 + 会话级/类型级双粒度征询
- [x] GuardModule 装配传同 registry
- [x] J 系测试解卡（import/包路径/yml list 形态）
- [x] guard 368 用例全绿

## Done

验证：`mvn -pl buzhou-guard test` 全绿。
