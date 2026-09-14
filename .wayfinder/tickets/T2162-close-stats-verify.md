---
id: T2162
title: 关闭耗时水位与失败分桶的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2161
created: 2026-09-14
---

## Question

如何证明计时埋点、失败分桶与幂等语义共存？

## Resolution

**用户常设授权 AFK（可推翻）**

测试计划：E2E 装配会话正常 close → closed=1、duration≥0、maxWatermark 单调；二次 close 幂等不重复计；onClose 抛异常的 observer → closeFailures=1 且清理继续（既有语义）；reset 归零。静态面前后归零防串扰。
