---
id: T2258
title: 核心 API 包类级 Javadoc 覆盖门的验证裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2257
created: 2026-09-15
---

## Question

M 会话第 4 轮：Javadoc 覆盖门如何验收？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决：`mvn -pl buzhou-core test -Dtest=CoreApiJavadocCoverageTest` 绿 + `mvn -pl buzhou-core compile` 绿——
① 32 类型补齐后覆盖门零缺失（一次全绿）；
② 门测试注解夹层感知（@Override/@Deprecated 等注解行不误报）；
③ 全量编译零破坏（批量插入不伤既有代码）。
