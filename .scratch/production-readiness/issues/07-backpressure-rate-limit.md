# 背压与多层限流

Type: grilling
Status: resolved

## Question

框架是否提供**背压与多层限流**?(参考文档十.1:全局 QPS/单用户 QPS/单 Agent QPS/模型维度限流;二.2 token 硬限流的流量侧)

需回答:
1. **做不做**——会话级/实例级并发上限、排队与拒绝策略、多维 QPS 限流是否框架职责(网关层限流明确不管)
2. **机制边界**——管控维度(并发会话数/每会话工具扇出/每模型 QPS/token 速率);排队语义(等多久、怎么拒)
3. **接缝**——虚拟线程执行模型下下游资源(DB 连接池、模型客户端连接)的保护点在哪;与动态预算(上下文 token 预算)的区分与协同;与 policy 四层配置的覆盖;限流事件进 observability

答题要求:同 03 号票(做/不做 + 机制边界 + 接缝;借鉴成熟实现——如 LangGraph Platform 的并发/队列语义、Sentinel/Resilience4j 模式——并注明来源)。

## Answer

(2026-08-11,grilling 收口,三问全采纳推荐)

**决策:做**——三维背压与限流,聚焦"出向保护+资源保护";网关入口 QPS 不做。

**机制边界(管什么/不管什么)**:
- **三维度**:① 实例级并发活跃会话上限(资源保护)② 每会话工具扇出上限(防单会话打爆下游)③ 每模型 **RPM+TPM 双桶**(出向 provider 配额保护;TPM 桶由 usage 采集数据流喂——超越 LangChain InMemoryRateLimiter 仅 RPM 的自认边界)
- **过载语义两档**:有界排队(默认,带超时,超时后拒绝)/ 快速失败;拒绝 = 调用方可重试的明确错误 + 事件进 observability。不做 interrupt/rollback(与 06 drain、租约单活跃语义冲突)
- **不管**:网关入口 QPS(K8s/网关标准职责);单用户/单租户配额(归 11 成本治理与 21 多租户——本票管瞬时速率,11 管累计闸门);分布式精确限流;429 自适应降速(AIMD,留 Spec 期评估)

**接缝**:
- 挂点三处:会话上限/排队 → spawn 入口(AgentRuntime);模型 RPM/TPM → Advisor 层(与 03 ResilienceAdvisor 同位——限流是韧性的前哨);工具扇出上限 → 执行脊柱 fan-out 处
- 限流状态:单进程内存 + 每实例配额配置(总配额/实例数由配置表达,如实文档化);不引 Redis 强依赖
- 与动态预算的区分(文档写清):动态预算管"单会话上下文窗口怎么分",本票管"跨会话速率与并发",正交
- 策略走 policy 四层配置

**借鉴**:
- LangChain InMemoryRateLimiter(令牌桶挂模型层;单进程/仅 RPM 的自认边界)— https://reference.langchain.com/python/langchain-core/rate_limiters/InMemoryRateLimiter
- LangGraph Platform multitask_strategy(并发裁决策略枚举;只借 reject/enqueue 两档)— https://github.com/langchain-ai/langgraph
- CrewAI max_rpm(数值化速率护栏)— https://docs.crewai.com/concepts/agents
