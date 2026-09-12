---
id: T962
title: store SPI 契约校验套件的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T961
created: 2026-09-13
---

## Question

契约套件对正确实现全绿？对走样实现（CAS 覆写默认实现竞窗不算——语义级）逐项红？自清理零残留？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 6 轮）：① InMemorySessionStateStore（真实实现）verify 全绿 9/9；② 故意注入走样实现（deleteIfValueMatches 无条件删 / scanByPrefix 漏过滤 / deleteSession no-op）——对应 Check 红且 passed()=false、失败名可读；③ 运行后 store 中无 `__contract__` 残留键；④ Report 不可变。`mvn -pl buzhou-core -am test` 全绿。
