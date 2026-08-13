# 08 — core / memory / spill / guard 四机制「做深做透」深度测试基线

**Impl-of:** [`.wayfinder/SPEC.md`](../SPEC.md) · **maps-to decision:** [T3](../tickets/T3-depth-definition-of-done.md)

**What to build:** 把「绿测试」升到「做深做透」——按 T3 DoD 基线，为 core / memory / spill / guard **各**加深度测试，并入各模块既有测试套件（不另开工程、`mvn -pl <module> -am test` 仍是统一入口）：

- **memory**：属性测试 / 不变式；预算压力下压缩信息不断崖丢失（微压缩占位符 + 证据指针可回查；九段式摘要 P0 死保 / P3 先砍的优先级正确）。
- **spill**：read_range 字节区间 / JSON path / 分页三种回读的正确性边界；失败语义非对称（读侧 offload 失败降级透传不阻断、写侧 onload 失败阻断调用）。
- **guard**：HITL → state → Attachment 事实闭环（Hook 确定性采集事实写 state、下一轮注入前渲染为 Attachment 进 prompt，不靠 LLM 自觉）。
- **core**：并行工具 fan-out / 按序回注 / 超时与取消传播；崩溃续跑（悬空调用修复：完全悬空剔除 / 部分悬空合成中断结果）；去重 / 幂等。
- **SPI 冻结**：`api` 子包契约稳定到可冻结的证明（复用 `AbstractBuzhouStoresContractTest` test-jar 模式）。

**Blocked by:** 决策票 **[T3](../tickets/T3-depth-definition-of-done.md)**（四模块深度判据须 grilling 定）+ **01**（依赖可解析）。

**Status:** ready-for-agent

- [ ] T3 DoD 基线已落（四模块各自深度判据清单）
- [ ] memory 深度测试并入 `buzhou-memory` 测试套件并绿
- [ ] spill 深度测试并入 `buzhou-spill` 测试套件并绿
- [ ] guard 深度测试并入 `buzhou-guard` 测试套件并绿
- [ ] core 深度测试并入 `buzhou-core` 测试套件并绿
- [ ] `api` 子包 SPI 契约稳定到可冻结的证明（复用持久化契约测试 test-jar 模式）
