---
id: T6118
title: S 会话 S9 Disruptor 环形缓冲的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6117]
created: 2026-09-24
---

## Question

S9 合同怎么逐一验绿？（spec 5008 / effort #5008 / S9）

## Resolution

**验证通过**：DisruptorRingBufferTest 五测全绿——两段式时序
（claim 未 publish 不可消费）；跨 wrap 完整性（容量 4 灌 10
条）；publishCursor 连续推进（乱序拒）；非 2 幂/越权/null
fail-fast。
