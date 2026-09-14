---
id: T2134
title: 事务计量守恒与异常透传的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2133
created: 2026-09-14
---

## Question

如何证明守恒式、异常透传分类与有界榜单？

## Resolution

**用户常设授权 AFK（可推翻）**

`InstrumentedUnitOfWorkTest` 六测全绿（`mvn -pl buzhou-core -am test`）：完成守恒（begun=completed+failed+inFlight）；失败原样上抛+IllegalStateException 入榜；deleteSession 透传+双重载计量；**同类失败并键累计+榜容量不超限**；reset 归零；null delegate fail-fast。
