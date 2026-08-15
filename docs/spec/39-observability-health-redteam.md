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
