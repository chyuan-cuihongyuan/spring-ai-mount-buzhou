---
id: T6198
title: S 会话 S49 Pairing Heap 配对堆的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6197]
created: 2026-09-25
---

## Question

S49 合同怎么逐一验绿？（spec 5048 / effort #5048 / S49）

## Resolution

**验证通过**：PairingHeapTest 五测全绿——脚本序弹出；300 键
TreeMap 多重集圣像（初版递归覆盖兄弟链丢节点由其钉住改
对称版）；meld 所有权清空+有序弹出；双实例同出序；fail-fast。
