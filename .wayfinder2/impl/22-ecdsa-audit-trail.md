# 22 — guard · ECDSA 签名审计链（IETF AAT + JCS）

**What to build:** 「授权发生过、决策链完整」防篡改地证明：AAT 格式审计记录 + prev_hash 链（RFC 8785 JCS）+ 可选 ECDSA P-256 签名（P1363 输出），覆盖 HITL 裁决、taint 写门、记忆写操作。

**Blocked by:** None — can start immediately.（记忆写/taint 裁决的接入点随 12/21 联动补挂，链本体独立）

**Status:** ready-for-agent

- [ ] 审计记录 11 字段（record_id/timestamp/agent_id/agent_version/session_id/action_type/action_detail/outcome/trust_level/parent_record_id/prev_hash）
- [ ] 链语义 `prev_hash(N)=SHA-256(JCS(record(N-1)))`——**JCS 规范化自写约 200 行、零新依赖**
- [ ] 签名可选 ECDSA P-256：去 signature 字段 JCS→SHA-256→签名→Base64url **IEEE P1363 r||s 64 字节**（JDK DER 转换约 30 行）
- [ ] session 收尾 session_hash
- [ ] 覆盖：HITL allow/deny/ask 裁决（先落）+ 记忆写/taint 写门裁决（接入点）
- [ ] 端到端：篡改任一记录→链校验失败；无签名模式（仅 hash 链）可用
- [ ] spec 07（Hook 护栏）同步

> spec 12 §guard-22；[T50](../tickets/T50-guard-ecdsa-audit-trail.md)。源：IETF AAT 草案注记（draft-sharif-agent-audit-trail-00）。
