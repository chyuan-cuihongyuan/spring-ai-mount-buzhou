# 05 — 提供真正可运行的 `src/main` demo

**Impl-of:** [`.wayfinder/SPEC.md`](../SPEC.md) · **maps-to decision:** [T4](../tickets/T4-runnable-main-demo.md)

**What to build:** 一个真正可运行的 `src/main` demo 入口（而非只有 JUnit）——最小覆盖多轮 + 工具 + 渐进式压缩，可选追加 Spill 回读 / HITL 门禁；无 API key 时也能跑（占位 / 可替换 stub）。demo 的行为**同时被 JUnit 断言**，由默认 `mvn verify` 兜回归。这是 core 端到端可用性的证明——demo 失败即暴露 core 缺口。README「方式三 纯编程式」那段代码须经验证真跑得通。

**Blocked by:** **01**（依赖可解析，demo 才能在干净环境复现）+ 决策票 **[T4](../tickets/T4-runnable-main-demo.md)**（demo 形态 / 模型接入 / 最小可跑路径须 prototype 拍板）。

**Status:** ready-for-agent

- [ ] T4 prototype 决策已落（形态 / 模型接入方式 / 最小可跑路径）
- [ ] 提供 `src/main` 入口，一条命令（`spring-boot:run` 或 `java -jar`）可跑
- [ ] 无 key 时以占位 / stub 跑通「多轮 + 工具 + 压缩」最小链
- [ ] demo 行为被 JUnit 断言，默认 `mvn verify` 兜回归
- [ ] README「方式三 纯编程式」代码经验证真跑得通（跑不通则修代码或修文档）
