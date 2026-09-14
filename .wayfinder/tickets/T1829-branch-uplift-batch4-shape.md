---
id: T1829
title: R11 分支批次 4 选题与形态（ThinkingChainExtractor × DefaultSpanHandle 边缘分支）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 11 轮：批次 4 候选（ThinkingChainExtractor 10 missed / DefaultSpanHandle 9 missed / ToolGraphAnalyzer 11）如何选题？ObservabilityAdvisor 流式 harness 如何处置？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 11 轮 = effort #1210 / spec 1210 / impl 913）：

1. **本批 = 两小类边缘分支**：ThinkingChainExtractor（ctor extraKeys 过滤链/stringOf 非 String/omitted 字符串形态）+ DefaultSpanHandle（attributes(Map) 批量导入从未被调用/attribute null 键/双 close 幂等/显式终态优先）。
2. **ObservabilityAdvisor 流式 harness 单列 R12**：68 missed 需从零搭 Spring AI 流式测试基建（StreamAdvisorChain stub/ChatClientRequest.builder/ChatResponse 构造），议程精确化入 map。
3. **ToolGraphAnalyzer**（11 missed）批次 5 候选（图分析输入形状需读 230 行源码）。
4. **形态**：DefaultSpanHandle 经 8 参公开构造 + RecordingBase（super 双参）录制；AssistantMessage 元数据注入沿 builder.properties 先例。
