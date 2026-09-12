---
id: T1037
title: MessageStore SPI 契约校验套件的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

MessageStore（append/load/deleteSession）是归档 saga/导入还原的数据底座——语义走样（deleteSession no-op、乱序返回）静默劣化。契约套件同构扩散怎么落？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 44 轮 = effort #744 / spec 743 / impl 546）：`MessageStoreContract`（core/spi，spec 705 同构）——四项有序契约：append/load 往返保序、未知会话空读、多次追加保序（时序不倒置）、deleteSession 幂等清场；Report/Check 不可变；`__contract__` 探针自清理；主源码零 JUnit。
