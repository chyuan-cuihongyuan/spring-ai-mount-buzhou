---
id: T2104
title: ToolResultSizeHistogram 分桶/守恒/失败隔离的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2103
created: 2026-09-14
---

## Question

如何证明边界落桶、守恒式与失败隔离？

## Resolution

**用户常设授权 AFK（可推翻）**

`ToolResultSizeHistogramTest` 三测全绿（`mvn -pl buzhou-core -am test`）：
1. E2E 六尺寸（10B/300B/2KB/5KB/20KB/100KB）各落一桶——经 HookedToolCallback 真实执行路径（脚本模型驱动 toolCall 六轮），successes=6、totalBytes 精确、executed 守恒；
2. 失败隔离：抛异常工具经错误即反馈通道 Turn 不死，failed=1 且不入字节分布（totalBytes=0）；
3. resetForTest 全零（直调 afterTool 最小替身）。
