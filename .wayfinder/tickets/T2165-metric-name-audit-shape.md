---
id: T2165
title: 指标命名校验器（MetricNameAudit）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 32 轮：指标命名自查工具的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：命名规则只在 starter 测试门（MetricNamingGuardTest NAME_RULE）——机制作者无即时自查工具。

形状裁决：MetricNameAudit 纯函数（core/metrics）——validate→NameVerdict(violations 闭集 EMPTY/WHITESPACE/PREFIX/SEGMENT_EMPTY/SEGMENT_CASE:段/SEGMENT_CHARS:段，首违不短路)+compliant 派生；段规则与门同源（^[a-z][a-z0-9-]*$+buzhou. 前缀）；运行期埋点接驳另轮。

Out of scope：starter 门单源化；tag key 规则；写入拦截。
