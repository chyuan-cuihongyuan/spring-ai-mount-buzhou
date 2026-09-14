---
id: T1549
title: read_file 读量水位与拒绝分桶读面（ReadFileStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1547
created: 2026-09-14
---

## Question

J 会话第 47 轮：tools/file 域读侧的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：spec 1046 Out of Scope 预告的 ReadFileTool 读侧分轴顺延——文件不存在、超 8MB、沙箱拒绝、异常兜底当前全部静默返回字符串，读吞吐与拒绝原因分布不可见；读/写对称计量（DatadogDDStats read/write 对称 tag 思想）。

形状裁决：`ReadFileTool` 内静态 `AtomicLong` 六计数——attempts（call 入口）/ reads（成功整读）/ bytesRead（UTF-8 字节累计）/ notFileRejects（不存在或非普通文件）/ oversizeRejects（超 8MB 预检）/ failures（catch 兜底，含沙箱拒绝）；嵌套 `record ReadFileStats`（totalRejects 派生）+ `stats()` + `resetForTest()`。守恒 `attempts = reads + notFileRejects + oversizeRejects + failures`。静态面理由同 WriteFileStats（R46 先例）。

Out of scope：不内建 offset/limit（瘦 Schema 原则，范围读取归 read_range）；不按 path 分桶（敏感面红线）；不改 call() 返回语义。
