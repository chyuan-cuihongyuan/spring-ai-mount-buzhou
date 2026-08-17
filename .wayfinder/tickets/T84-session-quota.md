---
Type: task
Status: closed
assignee: zcode
blocked-by: T83
---
## Question

per-session 配额限流怎么做？现状：ModelRateLimiter 是全局 per-model 单进程桶，单会话可饿死全局；无每日/每会话配额。决策点：配额维度（turns/tool-calls/tokens per session per day？）、计数存放（内存窗口+事件可见，分布式显式不做）、超配额行为（Block+结构化事件 vs 异常）、与 T83 计量的关系（复用累计计数器）。产出 spec 16 增量 + impl 59。

## Resolution

AFK 自决（授权同 T81，可推翻）：

1. **限流器进程级修正**（T81 勘察发现的语义缺陷）：ModelRateLimiter 从 customize() 内建移到 configure() 创建（进程级、customizer 闭包共享）——RPM/TPM 是进程级容量，每会话分桶会让 N 会话 = N 倍限额。
2. **配额维度**：`buzhou.resilience.session-quota.{turns-per-day, tool-calls-per-day, tokens-per-day}`（null = 不限；UTC 自然日固定窗口）。tokens 不复用 T83 累计键（那是会话生命周期累计、无日窗）——quota hook 在 afterModel 自行按日键累计（与 T83 同源提取 usage，键不交叉）。
3. **计数存放**：SessionStateStore 单键单维度 `"<epochDay>:<count>"`（读时日不符即重置——无需清理过期键，跨崩溃持久化）；分布式配额显式 out-of-scope（单进程语义，多实例=N 倍额度，T99 文档化）。
4. **超配额行为**：Block + 结构化事件 `quota.exceeded`（dimension/limit/value/day）——不抛异常（对齐 runaway/budget 硬顶语义：受控终态文本）；轮次配额在 beforeTurn 拦截、工具配额在 beforeTool 拦截、token 配额在 beforeModel 拦截（读当日已累计）。
5. **装配**：resilience 模块经 `RuntimeConfig.merge(hooks(quotaHook), assemblyCustomizers(...))` 贡献 hook（有配额维度才挂）；quota hook order 1150（budget 1100 之后）。
6. **观测**：ResilienceStats.quotaRejections + 指标 `buzhou.resilience.quota-rejected`（dimension tag）+ 事件走 ctx.emitEvent（SessionEvent 通道）。
