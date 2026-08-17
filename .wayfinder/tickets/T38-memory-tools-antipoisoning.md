---
id: T38
title: memory · memory-as-tools 自愈记忆 + 防投毒
type: task
status: closed
assignee: ""
blocked-by: T28
created: 2026-08-14
---

## Question

让 agent 自己修正压缩/摘要错误（memory-as-tools）会不会打开 Unit 42 记忆投毒攻击面？如何暴露写工具才安全？事实源：Letta（24,230★：`core_memory_replace` **精确匹配 + 多处命中抛唯一性错误** + 只读 block 拒绝——出现次数检查即防静默覆写）；Unit 42（注记：PoC 经**会话摘要 LLM 调用**投毒——工具输出是摘要 prompt 唯一攻击者可控槽位、伪造 `</conversation>` 逃逸、写入持久记忆可存 365 天；其缓解仅输入预处理/Guardrails/白名单/日志，**未提 taint/写确认**）。

## 待定决策（研究推荐已备）

1. 暴露 `revise_summary_section(section_id, old_text, new_text)`：**精确匹配 + 唯一性检查 + 类型化错误 `EDIT_NOT_FOUND`/`EDIT_AMBIGUOUS`**；P0 段只读锁——采纳（Letta 机制）。
2. **provenance + taint 位**：每次写入带来源 message id；evidence 源自工具输出的内容标 untrusted，未经脱敏不得进摘要正文、只进 scope 受限 evidence 区——采纳（**防投毒水位超越 Unit 42 公开建议**）。
3. 写操作全量审计日志（衔接 T50 ECDSA 链）——采纳。
4. 是否暴露 `archival_insert/search` 类工具面——本轮最小集只做 revise，archival 由 T41 三模搜承担——spec 确认。

依据：`docs/research/oss-perfect-tier23.md` §3.1（3–5 天，ROI 高）。

## Resolution

**Ratify 推荐 → [docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md) §memory-10**（用户常设授权 2026-08-14 ratify、可推翻）。revise_summary_section 精确匹配+唯一性错误+P0 只读锁；provenance+taint 位超越 Unit 42 公开缓解水位。
