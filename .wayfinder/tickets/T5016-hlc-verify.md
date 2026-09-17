---
id: T5016
title: Q 会话 R8 混合逻辑时钟的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5015]
created: 2026-09-18
---

## Question

R8 合同怎么逐一验绿？（spec 3007 / effort #3007 / R8）

## Resolution

**验证通过**：HybridLogicalClockTest 九测全绿——同墙百 tick 严格
递增、墙跳 (100,1)→(200,0) 仍单调、远端 (500,3) 吸收 (500,4)+
后续贴基线、三路同墙 max+1 双向、远端落后无感、物理回拨 40 不倒、
因果跨钟传递严格小于、双钟交错互投 50 轮步步增、Hlc 序四路。
