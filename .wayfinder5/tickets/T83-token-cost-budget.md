---
Type: task
Status: open
blocked-by:
---
## Question

Token/成本计量与预算闸怎么做？现状：usage 只到 per-call/per-turn span（ObservabilityAdvisor 写 span+Micrometer），无会话累计、无货币成本、RunawayHook 闸门只有步数/工具数。借鉴：OpenAI Agents SDK /cost、LangSmith cost per trace、Claude Code 会话成本显示。决策点：会话级累计的存放（SessionStateStore vs ObservabilityStore vs 新 SPI）、成本价目配置形态（per-model 价格表，默认无价=不计金额）、token 硬顶进 RunawayHook 四层硬顶之外的第五层、事件/指标/dashboard 可见性。产出 spec 16 + impl 58。
