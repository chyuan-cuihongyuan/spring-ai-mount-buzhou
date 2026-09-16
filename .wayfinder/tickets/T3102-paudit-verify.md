---
id: T3102
title: P 会话 2000 系对账门的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3101]
created: 2026-09-17
---

## Question

PSession2000LedgerAuditTest 四断言在 R1 自举时怎么全绿落地？（spec 2000 / effort #2000 / R1）

## Resolution

**自举验证通过**：spec 2000 落盘 + README 行登记 + T3101/T3102 票对 +
impl 1551 切片四件套齐整后，starter 模块单测
`mvn -pl buzhou-spring-boot-starter test -Dtest=PSession2000LedgerAuditTest`
全绿——起点断言 spec[0]=2000 命中本 spec，公式推值与文件系统事实一致；
后续每轮 commit 前复跑该测试即知漏登。
