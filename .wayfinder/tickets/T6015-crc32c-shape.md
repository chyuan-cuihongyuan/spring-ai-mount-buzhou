---
id: T6015
title: R 会话 R8 CRC-32C 的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

传输/落盘完整性的软件基准校验怎么做？（spec 4007 / effort #4007 / R8）

## Resolution

**Crc32C（core/message，纯静态）**：Castagnoli 0x1EDC6F41 反射表
驱动（init/xorout 全 1）——标准检验向量锚定的跨平台对账锚点
（硬件 SSE4.2 路径等价性基准）；compute 全量/区间 + verify 到达
校验。与编码件成对（紧凑 vs 一致）。
