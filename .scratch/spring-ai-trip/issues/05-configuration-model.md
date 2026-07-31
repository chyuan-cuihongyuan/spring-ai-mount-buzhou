# 配置体系与策略模型

Type: grilling
Status: open
Blocked by: 03

## Question

统一配置模型怎么设计：每个机制独立开关（safe by default，默认开哪些？）；按工具粒度的策略（压缩策略、Spill 阈值、永不压缩清单）与按 `(appId, agentName)` 粒度的绑定如何表达？`application.yml` 的 schema 长什么样？配置中心（Nacos/Apollo/QConfig）驱动的运行时变更是内置抽象还是留给业务？配置优先级（默认 < 全局 < agent 级 < 工具级）如何叠加？
