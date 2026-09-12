---
id: T1010
title: 健康时间线 JSONL 压缩线装配的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T1009
created: 2026-09-13
---

## Question

压缩线声明 → 跨线档 .gz 可解、file.1 明文？缺省 0 全明文零回归？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 30 轮）：① compressFrom=2 写 5 条 → file.2.gz 存在且 GZIP 解压含 memory 事件、file.2 明文不存在、file.1 明文存在；② 缺省 0 → 全明文；③ 既有 HealthTimelineJsonl 用例零回归。
