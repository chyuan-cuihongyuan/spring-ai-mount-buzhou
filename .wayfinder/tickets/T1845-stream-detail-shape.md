---
id: T1845
title: R19 选题——Advisor 流式语义定向补测（快照提取/TTFT 幂等/omitted-only/空信号）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 19 轮：流式路径语义定向补测（不经行号、按行为语义选题）——captureInjectionSnapshot 的 ToolResponseMessage evidence/spill 提取、markFirstToken 幂等、omitted-only chunk、空信号防御如何落地？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 19 轮 = effort #1218 / spec 1218 / impl 921）：

1. **ToolResponseMessage 快照提取**：ToolResponseMessage 构造受限（protected）→ 测试内静态子类桥接（同包可见），Prompt 注入 USER+TRM 双消息 → 快照 SnapshotMessage 的 evidenceId/spillUri 经正文模式匹配填充（EVIDENCE_PATTERN/SPILL_PATTERN 首次真实断言）+ firstMatch 无匹配 null 分支（无模式文本）。
2. **TTFT 幂等**：两个含内容 chunk → STREAM_FIRST_TOKEN 恰一次（CAS 幂等分支）。
3. **omitted-only 流 chunk**：thinking_omitted=true 无文本 → extractor 返回 content="" → lambda blank 分支 → 无 THINKING 事件、无内容信号。
4. **空信号防御**：Flux.empty（仅 complete 信号）→ doOnEach resp=null 分支 → 正常 complete 关闭 span。
5. **边界**：recordTpotIfNeeded 的 perTokenNs≤0 分支（计时依赖）不硬凑；不改主代码。
