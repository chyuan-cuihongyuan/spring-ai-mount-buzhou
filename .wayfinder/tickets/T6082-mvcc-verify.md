---
id: T6082
title: R 会话 R41 MVCC 快照可见性的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6081]
created: 2026-09-24
---

## Question

R41 合同怎么逐一验绿？（spec 4040 / effort #4040 / R41）

## Resolution

**验证通过**：MvccVisibilityTest 六测全绿——先提交可见/快照
后创建不可见/在飞创建者新旧照分立/中止创建者永不可见/删除
四象限/repeatable read 双读一致 + 未知与重复收尾 fail-fast。
