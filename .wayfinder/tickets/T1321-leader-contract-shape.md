---
id: T1321
title: LeaderElector 契约校验套件的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 53 轮：LeaderElector（spec 331 选主 SPI：tryAcquireOrRenew/resign/inspect + 围栏纪元单调）的契约套件是否有缺口？与对方 R40「选举竞争读数」分轴。

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 53 轮 = effort #954 / spec 954 / impl 699 续）：契约缺口成立（正确性契约 vs 对方读数面分轴）。落点 `LeaderElectorContract`（spi 静态 verify 范式）五项检查——①空闲获取进入新纪元且 leader=true ②已持有人重入幂等（同 epoch 续期）③他人持有时返回跟随态（leader=false）④resign 后空位可被获取（新纪元或同候选重取）⑤inspect 反映持有者/空位（空位 epoch=0）。配套内存实现接入测试（core 测试域 InMemoryLeaderElector 过五项）。
