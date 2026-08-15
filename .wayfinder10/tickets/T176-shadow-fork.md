---
Type: task
Status: open
---
## Question

配置开关（默认关）下 turn 完成后异步 fork 路径到 shadow 模型执行同输入：结果不回注用户、只记 span 对照指标（latency/token/cost delta）；并发与预算护栏（shadow 消耗计入独立 shadow 预算池，池尽即停）。验证：开关单测 + 隔离性断言（不影响主链路）。
