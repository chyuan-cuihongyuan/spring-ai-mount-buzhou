---
id: T6139
title: S 会话 S20 热点 Key 探测器的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

热点 key 怎么确定性采样观测并告警？（spec 5019 /
effort #5019 / S20）

## Resolution

**HotKeyDetector（core/metrics）**：确定性采样——record 按
全局序号 mod sampleRate 命中计数（无随机），采样计数达
hotThreshold 即热点；hotKeys 字典序全集；畸形定构/null
fail-fast。
