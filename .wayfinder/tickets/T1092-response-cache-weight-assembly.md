---
id: T1092
title: 响应缓存权重预算 yml 装配的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question
737 原语无装配——yml 怎么开？

## Resolution
**用户常设授权 AFK（可推翻）**

决策（G 会话第 46 轮 = effort #745 / spec 745 / impl 646）：ResponseCache record 扩 maxWeightChars（Long 默认 0=关；@ConstructorBinding+3 参兼容——R39/R48 坑规避；负值 fail-fast）；ResilienceModule 装配点透传；yml+metadata 登记。
