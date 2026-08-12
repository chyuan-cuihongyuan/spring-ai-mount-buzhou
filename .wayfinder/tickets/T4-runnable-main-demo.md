---
id: T4
title: 提供真正可运行的 src/main demo（而非只有测试用例）
type: prototype
status: open
assignee: ""
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
- **blocked-by [T1](T1-ci-red-remotely-green-locally.md)**：依赖可解析，demo 才能在干净环境复现。
- 偏 prototype（HITL）：demo 形态需用户拍板。

## Resolution

<!-- prototype 后填写：demo 形态决策 + 入口 + 跑法，作为 asset 链接 -->
