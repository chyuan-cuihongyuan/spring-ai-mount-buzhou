---
id: V8009
title: V 会话 V5 GlobMatcher 的形状裁决
type: task
status: closed
assignee: zcode-v
blocked-by: []
created: 2026-09-29
---

## Question

配置面通配匹配怎么既全又轻？（spec 8004 / effort #8004 / V5）

## Resolution

**GlobMatcher（core/metrics）**：四类 token（星/问/类/字面）
前端解析+星号单候选位回溯线性匹配；类支持区间与 `!` 否定、
首 `]` 字面量；未闭合 `[` fail-fast。
