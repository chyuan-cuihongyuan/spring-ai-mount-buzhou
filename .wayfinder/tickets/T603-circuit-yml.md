---
Type: task
Status: closed
---
## Question

circuit 属性组（四参默认与 Config 同口径）+ ToolCircuitBreakerHook 装配 bean。

## Resolution

done（2026-09-01）：impl-329；BuzhouToolsProperties.Circuit + autoconfig @Bean
（ConditionalOnProperty enabled，BuzhouHook 自动收集）。
