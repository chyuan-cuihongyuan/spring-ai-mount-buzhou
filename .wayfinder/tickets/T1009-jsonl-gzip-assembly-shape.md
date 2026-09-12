---
id: T1009
title: 健康时间线 JSONL 压缩线装配的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

RollingJsonlWriter 压缩线（spec 712）只有编程构造——健康时间线 JSONL 的 yml 声明式入口缺失。

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 30 轮 = effort #729 / spec 729 / impl 532）：BuzhouHealthTimelineProperties 增 exportCompressFrom 组件（缺省 0=关；1 非法 fail-fast——file.1 恒明文 delaycompress）；HealthTimelineJsonl 增 4 参构造重载；buzhouHealthTimelineJsonl bean 透传。既有便捷构造向后兼容。
