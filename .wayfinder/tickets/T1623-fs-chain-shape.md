---
id: T1623
title: fs 全链路四读面组合测试轮的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1619
created: 2026-09-15
---

## Question

J 会话第 84 轮：跨读面组合的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：R45 沙箱判定/R46 写/R47 读/R51 黑名单四读面在真实工作流中协同（写→读→越界读→受控命令）——各轮独立验证，**链路组合下的计数一致性**无验证。纯测试轮第三弹。

形状裁决：新增 `FsChainReadoutTest`（buzhou-tools）——写文件（writes+bytes）→ 读回（reads+bytes 对称）→ 越界读（read failures）→ 越界写（write failures）——断言四读面各自计数与跨面对称恒等在链路后仍保持。零生产改动。
