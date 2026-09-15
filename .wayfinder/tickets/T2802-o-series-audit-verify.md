---
id: T2802
title: O 系对账门的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2801]
created: 2026-09-16
---

## Question

对账门四断言（票对/impl/README 覆盖/严格递增）在真实仓库工件上全绿吗？（spec 1800 / effort #1800 / R1）

## Resolution

**4 断言全绿**（mvn -pl buzhou-spring-boot-starter
test -Dtest=OSession1800LedgerAuditTest）。首跑 1 红「spec 1800 缺 impl 切片
1401」——公式断言自身生效的即席自证，补 impl 1401 后转绿。
