---
id: U7243
title: U 会话 U22 WriteAheadLog 的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

崩溃重放怎么不吐坏数据？（spec 7021 / effort #7021 / U22）

## Resolution

**WriteAheadLog（core/fs）**：写侧 CRC32 存储重放重算比对；分段滚动；LSN 单调；段镜像审计面。
