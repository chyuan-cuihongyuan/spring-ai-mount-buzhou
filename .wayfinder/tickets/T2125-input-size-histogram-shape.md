---
id: T2125
title: 工具入参字节直方（ToolInputSizeHistogram）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 13 轮（换题轮）：工具入参体量分布面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察换题：原题 R49 与 SummaryDegradeReasons（H 844）+CompactionRatioStats 覆盖——换入 R41 通用化（全部工具入参侧，与 1401 结果侧对称）。

形状裁决：ToolInputSizeHistogram implements BuzhouHook（opt-in beforeTool 单点）——arguments Jackson 序列化 UTF-8 字节落同款五幂次边界桶+溢出+executed/totalBytes，守恒 executed=Σbuckets；钩内自序列化成本口径入档（仅注册者承担）；测量点=链前原始入参（replaceArguments 之前）。

Out of scope：工具名 tag；结果侧重复；MCP 单独分面。
