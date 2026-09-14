---
id: T2103
title: 工具结果字节直方分桶（ToolResultSizeHistogram）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 2 轮：工具结果尺寸分布读面的形状与挂点选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：ToolInFlight（J 1005）/ timer（108/700）/ 慢榜（1001）/ 错误签名（83）皆时延与异常维，结果体量维全空；HookedToolCallback 在 markExecured 后派发 afterTool 且 ctx.result() 可达——Hook 单点挂法零侵入（不必改 HookedToolCallback）。

形状裁决：`ToolResultSizeHistogram implements BuzhouHook`（core/hook，opt-in）——五幂次边界桶（256/1K/4K/16K/64K）+溢出桶，executed/failed/totalBytes 三总量；守恒式 successes=Σbuckets、executed=successes+failed；UTF-8 字节口径与 J R46–R47 文件工具对齐；afterTool 只读 CONTINUE。测量点口径显式：hook 链改写前原始产物。

Out of scope：per-tool tag 分桶（基数红线）；请求侧分布；无界精确分位。
