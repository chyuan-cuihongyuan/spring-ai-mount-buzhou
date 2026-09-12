---
id: T946
title: JSONL 轮转事件指标化的裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-13
---

## Question

spec 642 的 RollingJsonlWriter 有 rotations()/rotationFailures() 编程 getter，但装配部署（yml 声明路径的用户）看不到轮转发生过/失败过——「磁盘保护是否真在工作、轮转路径是否有病灶」运维盲。指标化吗？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 49 轮 = effort #600 / spec 648 / impl 501）：rotate() 成功发 `buzhou.jsonl.rotated`、失败发 `buzhou.jsonl.rotate-failed`（BuzhouMetricsHolder 全局面——库内默认 no-op 零开销，micrometer 装配后真实计数；tag file=文件名有界）。rotateIfNeeded 静态路径同发。getter 面保留（测试/编程消费方）。
