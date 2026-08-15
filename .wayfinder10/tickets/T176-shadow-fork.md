---
Type: task
Status: closed
---
## Question

配置开关（默认关）下 turn 完成后异步 fork 路径到 shadow 模型执行同输入：结果不回注用户、只记 span 对照指标（latency/token/cost delta）；并发与预算护栏（shadow 消耗计入独立 shadow 预算池，池尽即停）。验证：开关单测 + 隔离性断言（不影响主链路）。

## Resolution

spec 49 §A / impl-145 落地：buzhou.resilience.shadow.* 配置组（默认关）+ ShadowTrafficController
（进程级：并发信号量默认 2 + UTC 日预算默认 1000；护栏拦下计 skipped-concurrency/skipped-budget）
——主模型成功后异步裸 ChatModel 调用（不重放工具循环，副作用红线）、shadow.compared 对照事件
（primary/shadow 延迟 + deltaMs + tokens）+ shadow.calls{outcome} 计数；失败吞噬不拖主链路。
金丝雀路径与流式路径不探测（诚实边界入档 spec）。实现期纠偏：Shadow.effectiveEnabled 开关与
模型来源解耦（编程式路径无 bean 名名单）；ResilienceProperties 规范构造器唯一性修正。
5 新测试绿，resilience 96 全绿。
