# Spec 39 — 观测审计 / health 新维度 / 红队新面

> effort #8（T138–T140 / impl-111–113）。

## §A 红队新攻击面（T138 / impl-111，观察档）

- 两类新攻击面（promptfoo 词汇不可表达）以确定性对抗用例承载
  （examples `NewSurfaceAdversarialTest`，替身模型评 harness 行为）：
  - **multimodal-injection**：媒体引用内容携带越权指令——HITL 危险门语义不因输入
    通道改变（未授权执行仍被拦）。
  - **tool-result-injection**：工具结果携带「忽略之前指令」类载荷——以**数据形态**
    在场（ToolResponse 原文可见）、后续危险调用仍走门。
- 口径：先观察不进硬门；稳定后转 nightly 重放并按 baseline 定门（README 注记）。

## §B 观测背压审计（T139 / impl-112）

- **审计结论**：管线满队语义 = **阻塞背压而非丢弃**（`queue.put`）——at-least-once
  观测不丢是设计目标；代价是极端慢 store 时入队线程（会话主链）等待。
- 回归测试钉住语义：容量 1 + 慢 store（latch）下第 3 条 emit 阻塞、释放后零丢失。
- javadoc 显式化满队语义；runbook §7 增 `buzhou.observability.queue.wait` P95 告警
  （>500ms → 查 store 健康/批量参数）。
- **新事件源核对**：memory.compacted 直写 `observabilityStore().saveEvents` 与
  RunawayCounters 同款（T66 双写先例）——写入失败 lenient 已有
  （CompactionEventTest.listenerFailureNeverBreaksView）。
