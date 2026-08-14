---
Type: task
Status: open
blocked-by: T82,T85,T87,T88,T89,T90,T91
---
## Question

API 稳定性审计怎么做？决策点：public API surface 清单生成（模块 × public 类/接口清单落档 docs/api-surface.md）、关键接口 javadoc 补齐（AgentRuntime/AgentSession/Hook 链/SPI 五接口）、@since 标注规范（1.0.0 基线）、internal 包约定核查（public class in internal package 清单）、deprecation 政策写入 CONTRIBUTING。产出 spec 23 增量 + impl 75。
