---
id: T6140
title: S 会话 S20 热点 Key 探测器的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6139]
created: 2026-09-24
---

## Question

S20 合同怎么逐一验绿？（spec 5019 / effort #5019 / S20）

## Resolution

**验证通过**：HotKeyDetectorTest 四测全绿——采样确定性
（rate=10 恰每 10 次、第 10 次命中）；阈值触发对照；
hotKeys 字典序；畸形定构/null key fail-fast + 同序列回放。
