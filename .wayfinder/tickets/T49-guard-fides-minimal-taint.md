---
id: T49
title: guard · FIDES 最小 taint 信息流控制（标 + 写门校验）
type: task
status: closed
assignee: ""
blocked-by: T38
created: 2026-08-14
---

## Question

读写非对称的「形式正确性终点」：不可信数据未经审批不得流入写侧——taint 如何最小可行落地？事实源：MSRC FIDES 论文（arXiv 2505.23643，注记源、全文已取）：标签=join 半格（机密性/完整性积格）；传播=工具结果节点级挂标签→LLM 响应保守取输入 join→会话历史累积→工具执行前查 policy；**AgentDojo：策略开启后注入成功数 0、效用损失 4.5–16.2%**；逃生舱（隔离 LLM/变量隐藏）二期再议。

## 待定决策（研究推荐已备）

1. **最小可行 taint = 只标 + 写门校验**：读侧 hook 给工具/RAG 输出 Attachment 打 `TaintLabel`（枚举起步 TRUSTED/UNTRUSTED + 来源，与 T38 provenance 同源）——采纳。
2. LLM 响应 join 传播进会话状态；写门（物理阻断 + HITL 工具调用前）校验「上下文标签 ⊔ 实参标签」，失败走既有 session-state 授权 + TTL（= FIDES approver 的 Buzhou 等价物）——采纳（复用既有 hook→state→Attachment 链）。
3. 二期（变量隐藏/隔离 LLM/类型容量格）入 fog，MVP 落地后按效用损失实测再定——采纳。

依据：`docs/research/oss-perfect-tier23.md` §5.2（MVP 4–6 天，ROI 高：读写非对称的形式化升级）。

## Resolution

**Ratify 推荐 → [docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md) §guard-21**（用户常设授权 2026-08-14 ratify、可推翻）。最小 taint=只标+写门校验；LLM 响应 join 传播；二期（变量隐藏/隔离 LLM）留 fog。
