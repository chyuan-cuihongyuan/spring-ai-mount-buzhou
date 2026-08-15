---
Type: task
Status: open
---
## Question

限流/熔断冷却/配额 UTC 窗口/webhook 到期轮询全依赖系统时钟（全仓 main 仅 2 处注入 Clock），时间行为测试只能真实等待：时钟注入面切到哪些组件、什么形态（构造器/Builder 可选参）？
