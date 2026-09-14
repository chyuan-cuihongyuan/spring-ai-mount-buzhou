# 1503 — 核心 API 包类级 Javadoc 覆盖门

> 来源：M 会话第 4 轮 = effort #1503（impl 1106）。Spotless 类静态门思想 + 仓库「纪律变测试」先例（spec 213 绑定矩阵）。

## 背景

CLAUDE.md 规范「api 子包与 SPI 必须有 Javadoc」在实际分包（无 api 子包，公共 API 面 = core 内核 session/hook/exec/spi/observability/error 六包）下违例：192 个公共顶层类型中 32 个缺类级 Javadoc，含 AgentSession / AgentRuntime / BuzhouHook / HookResult / RuntimeConfig / HarnessToolCallingManager 等最核心 API。

## 目标

- 32 个类型逐一补类级 Javadoc（角色 + 关键语义 + spec/impl 引用）；
- `CoreApiJavadocCoverageTest`（core 测试，纪律变测试）：六包公共顶层类型必须有类级 Javadoc——注解夹层感知（Javadoc 与 public 声明之间允许注解行），新增公共类型不带 Javadoc 即 CI 红。

## 兼容性

纯注释 + 新测试，零行为变化。
