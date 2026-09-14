---
id: T2257
title: 核心 API 包类级 Javadoc 覆盖门的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by:
created: 2026-09-15
---

## Question

M 会话第 4 轮：core 内核公共 API 面的类级 Javadoc 覆盖缺口如何处置？

## Resolution

**用户常设授权 AFK（可推翻）**

现状实证（注解夹层感知扫描）：session/hook/exec/spi/observability/error 六包 192 个公共顶层类型中 32 个缺类级 Javadoc——含 AgentSession/AgentRuntime/BuzhouHook/HookResult/RuntimeConfig/HarnessToolCallingManager 等最核心 API（CLAUDE.md「api 子包与 SPI 必须有 Javadoc」规范违例；实际分包无 api 子包，规范落点即六内核公共包）。

形状：① 32 个类型逐一补类级 Javadoc（角色 + 关键语义 + spec/impl 引用，一句至三句）；② 纪律变测试（spec 213 绑定矩阵先例）：CoreApiJavadocCoverageTest——六包公共顶层类型必须有类级 Javadoc（注解夹层感知：Javadoc 与 public 声明间允许注解行），新增公共类型不带 Javadoc 即 CI 红。思想源：Spotless/ErrorProne 类静态门 + 仓库「纪律变测试」先例。
