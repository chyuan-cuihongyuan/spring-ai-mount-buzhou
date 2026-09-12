---
id: T1042
title: 嵌入超限分批装饰器的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

批量嵌入超供应商单请求 cap 即 400——加切分装饰器吗？要不要并发合并窗？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 22 轮 = effort #721 / spec 721 / impl 621）：`ChunkingEmbeddingModel` 实现 EmbeddingModel 仅覆写 call——instructions 超 maxBatchSize 切块顺序调 delegate、输出全局 index 重排拼接，≤max 直通；maxBatchSize≥1 fail-fast；default embed 方法经 call 自动受益。纯确定性无时序窗（并发合并另题）。usage 合并不做（分次计费口径本就分次）。
