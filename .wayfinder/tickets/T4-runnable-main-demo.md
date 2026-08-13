---
id: T4
title: 提供真正可运行的 src/main demo（而非只有测试用例）
type: prototype
status: closed
assignee: zcode
blocked-by: [T1]
created: 2026-08-13
---

## Question

- demo 演示哪些 core 机制（最小覆盖：多轮 + 工具 + 压缩，还是再加 Spill 回读 / HITL）？
- 用什么模型接入（真实 API key 还是占位/可替换 stub），如何在无 key 时也能跑？
- 最小可跑路径是什么（一条 `main` 类、`spring-boot:run`、还是 `java -jar`）？README「方式三 纯编程式」那段代码是否真跑得通？
- demo 同时是 core 端到端可用性的证明——它失败即暴露 core 缺口。

## Context

- 当前 `examples/` 有评测套件（`SummaryEvaluationTest`）与 `AtomicToolsIntegrationTest`，但缺一个能直接 run 的 `src/main` 入口。
- 依赖前提：[T1](T1-ci-red-remotely-green-locally.md)（已 closed）确证依赖从 Central 正常解析（CI 红是无关 OS 缺陷，见 [T10](T10-fix-ci-os-specific-defect.md)）；CI-badge 绿由 T10 追踪、不阻塞本 ticket 形态决策（本地可验证）。
- 偏 prototype（HITL）：demo 形态需用户拍板。

## Resolution

**决策（stub-first + 可插真 key）**：`examples/src/main/.../demo/BuzhouDemo.java` —— 纯编程式 `Buzhou.runtime(model, stores, config)` + `MemoryModule`，无 Spring Boot、无 API key 即跑。`run(ChatModel)` 接受任意模型：`main` 默认传确定性 `StubChatModel`（无 key、CI 可断言注入视图），真实模型可替换传入。

**最小可跑路径**：`main` 类（IDE 直跑 / `mvn -pl examples exec:java` / `java -cp`）。**形态**：预置 10 轮「排障 Agent」历史（每轮 `get_order_status` 大日志）→ 跑一轮触发**微压缩**（旧轮大工具返回 → evidence 占位符）→ `read_evidence` 回查原文。最小覆盖多轮 + 工具 + 压缩；Spill 回读 / HITL 见既有 `examples/` 其它 demo 测试。

**README「方式三」验证**：demo 与 README snippet 同 API（`Buzhou.inMemoryStores`/`Buzhou.runtime`/`spawn`/`chat`），互证真跑得通。

**验证（JDK 21 本机）**：`BuzhouDemoTest` ✓（断言压缩触发 + evidence 回查原文）；`main()` 实跑输出可见。examples/pom 为 src/main 加 compile-scope core+memory（消费侧、非 feature 互依）。

**实现切片**：[impl/05](../impl/05-runnable-main-demo.md)。
