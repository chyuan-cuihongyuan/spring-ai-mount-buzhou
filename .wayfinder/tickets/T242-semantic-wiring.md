---
Type: task
Status: done
blocked-by: T241-semantic-advisor.md
---
## Question

EmbeddingModel 装配（ObjectProvider 注入 Spring AI EmbeddingModel）；enabled=true 且
无 bean → BuzhouConfigurationException fail-fast 带修法（与 shadow 解析同口径）；
ResilienceProperties.SemanticCache 参数组（enabled/similarity-threshold/max-entries/ttl，
默认 false/0.95/128/1h）+ spring-configuration-metadata 键入档；默认关闭零行为变化。
