---
id: T50
title: guard · ECDSA 签名审计链（IETF AAT + JCS）
type: task
status: closed
assignee: ""
blocked-by: T28
created: 2026-08-14
---

## Question

「授权发生过、决策链完整」如何防篡改地证明？事实源：IETF Agent Audit Trail 草案（注记源：draft-sharif-agent-audit-trail-00；必填 11 字段 record_id/timestamp/agent_id/agent_version/session_id/action_type/action_detail/outcome/trust_level/parent_record_id/prev_hash；链=`prev_hash(N)=SHA-256(JCS(record(N-1)))`（**RFC 8785 JCS 强制**）；签名 ECDSA P-256 → Base64url **IEEE P1363 r||s 64 字节**非 DER；session 收尾 session_hash）。

## 待定决策（研究推荐已备）

1. 审计记录按 AAT 11 字段 + prev_hash 链；签名可选开（ECDSA P-256）——采纳。
2. Java 落点：JDK 内置 `SHA256withECDSA`（DER→P1363 转换约 30 行）；**JCS 规范化 JDK 无内置**——引入小型第三方实现或自写约 200 行——spec 定（倾向自写：零新依赖、规范小）。
3. 覆盖范围：HITL allow/deny/ask、taint 写门裁决（T49）、memory 写操作（T38）——采纳。

依据：`docs/research/oss-perfect-tier23.md` §5.3（3–5 天，ROI 中高：纯本地、给确定性事实采集加防篡改）。

## Resolution

**Ratify 推荐 → [docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md) §guard-22**（用户常设授权 2026-08-14 ratify、可推翻）。AAT 11 字段+prev_hash 链；JCS 自写（零新依赖）；ECDSA P-256 可选、P1363 输出。
