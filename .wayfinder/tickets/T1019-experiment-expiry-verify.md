---
id: T1019
title: 实验到期自动停验证
type: task
status: closed
assignee: zcode-g
blocked-by: [T1018]
created: 2026-09-12
---

## Question

到期语义与口径分离如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 10 轮 = effort #709）：MutableClock——①未到期正常入变体、推进后返回 null+`__expired__` 计数+expiredExperiments 可见；②无到期声明实验推进后照常；③expiresAt 含 null 值构造拒绝+expiresAt(name) 往返。buzhou-core 全模块零回归（C 会话排除集）。
