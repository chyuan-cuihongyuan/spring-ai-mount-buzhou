---
id: T6002
title: R 会话 R1 4000 系对账门的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6001]
created: 2026-09-23
---

## Question

R1 合同怎么逐一验绿？（spec 4000 / effort #4000 / R1）

## Resolution

**验证通过**：RSession4000LedgerAuditTest 四断言全绿——空档起步
（仅 spec 4000 存在）起点=4000、shape/verify 票对（T6001/T6002）就位、
impl 2101 切片就位、README 含 4000 行（覆盖门与 SpecCoverageTest
双向绿，starter 模块测试退出码 0）。
