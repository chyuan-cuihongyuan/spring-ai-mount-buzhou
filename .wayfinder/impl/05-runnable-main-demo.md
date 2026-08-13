# 05 — 提供真正可运行的 `src/main` demo

**Impl-of:** [`.wayfinder/SPEC.md`](../SPEC.md) · **maps-to decision:** [T4](../tickets/T4-runnable-main-demo.md)

**What to build:** 一个真正可运行的 `src/main` demo 入口（而非只有 JUnit）——最小覆盖多轮 + 工具 + 渐进式压缩，可选追加 Spill 回读 / HITL 门禁；无 API key 时也能跑（占位 / 可替换 stub）。demo 的行为**同时被 JUnit 断言**，由默认 `mvn verify` 兜回归。这是 core 端到端可用性的证明——demo 失败即暴露 core 缺口。README「方式三 纯编程式」那段代码须经验证真跑得通。

**Blocked by:** **01**（依赖可解析，demo 才能在干净环境复现）+ 决策票 **[T4](../tickets/T4-runnable-main-demo.md)**（demo 形态 / 模型接入 / 最小可跑路径须 prototype 拍板）。

**Status:** done (assignee: zcode)

- [x] T4 prototype 决策已落 —— stub-first + 可插真实模型（`run(ChatModel)` 接受任意模型，`main` 默认传 `StubChatModel` 无 key 可跑）；最小可跑路径 = `main` 类（IDE / `java -cp` / `mvn exec:java`）
- [x] 提供 `src/main` 入口（`examples/src/main/.../demo/BuzhouDemo.java`），一条命令可跑
- [x] 无 key 时以 stub 跑通「多轮 + 工具 + 压缩」最小链 —— `main()` 实测输出可见：微压缩触发 `true`、`read_evidence` 回查原文含订单号
- [x] demo 行为被 JUnit 断言（`BuzhouDemoTest`），默认 `mvn verify` 兜回归 —— 本机 JDK 21 实跑 `Tests run: 1, Failures: 0`
- [x] README「方式三 纯编程式」代码经验证真跑得通 —— demo 用同一 API（`Buzhou.inMemoryStores` + `Buzhou.runtime(model, stores)` + `spawn` + `chat`）

## Resolution

**交付**：`examples/src/main/.../demo/BuzhouDemo.java`（纯编程式 `Buzhou.runtime` + `MemoryModule`，无 Spring Boot、无 API key）+ `BuzhouDemoTest`（同 `run()` 逻辑的 CI 断言）。examples/pom 为 demo 的 src/main 新增 **compile-scope** `buzhou-core` + `buzhou-memory`（examples 是消费侧、非 feature 互依，09 模块工程硬约束不受影响；既有测试仍用 test-scope 聚合依赖）。

**形态（T4 stub-first + 可插真 key）**：`run(ChatModel)` 接受任意 `ChatModel`——`main` 传确定性 `StubChatModel`（无 key、CI 可断言注入视图）；真实模型可替换传入（真实模型不暴露注入视图，compaction 靠模型行为体现）。预置 10 轮「排障 Agent」历史（每轮 `get_order_status` 大日志），再跑一轮触发**微压缩**：旧轮大工具返回 → evidence 占位符，`read_evidence` 回查原文。

**验证（JDK 21，本机）**：
- `mvn -pl examples -am test-compile` ✓
- `BuzhouDemoTest` ✓ `Tests run: 1, Failures: 0`（断言：压缩触发、evidence-id 可解析、回查原文含 `ORDER_ID`+`ERROR_CODE`）
- `main()` 实跑输出可见（微压缩 `true` + evidence 回查 `true` + 注入视图片段）

**README「方式三」**：demo 与 README snippet 同 API（`inMemoryStores`/`runtime`/`spawn`/`chat`），互为验证。

**注**：本 Windows 主机可编译+跑此 demo（不涉 `/bin/sh`、不读中文资源、无 CRLF 敏感断言）。
