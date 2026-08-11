# 多模型路由:负载均衡/主备/降级链/静态兜底

Type: grilling
Status: resolved
Blocked by: 03

## Question

> **03 已决(2026-08-11)**:韧性层 M2(熔断+兜底响应)与本票同炉设计——"熔断打开→切备模型"是同一连续体;03 已定执行点为 ResilienceAdvisor、错误分类已归一化(限流/鉴权/内容/网络/未知),本票在此基础上答路由问题。

是否提供**多模型路由层**?(参考文档二.4 多 LLM 负载均衡与故障转移、十三.1 LLM 全故障静态兜底)

需回答:
1. **做不做**——主模型故障切备模型、贵模型降级便宜模型、多服务商负载均衡、全故障时静态兜底应答,哪些归框架
2. **机制边界**——切换触发条件、会话内切换 vs 会话间切换、路由规则(固定链/健康度动态)归谁定义
3. **接缝**——与 03 韧性层的关系(fallback 是韧性的一种还是独立机制);**换模型后的上下文兼容性**:token 预算重估、工具 Schema 差异、思维链格式差异怎么处理;与 policy 四层配置的覆盖关系;与记忆压缩/九段摘要的兼容性

答题要求:同 03 号票(做/不做 + 机制边界 + 接缝;借鉴成熟实现并注明来源)。

## Answer

(2026-08-11,grilling 收口,四问全采纳推荐)

**决策:部分做**——failover 链 + 降级链 + 静态兜底归框架(与 03 韧性层 M2 熔断同炉);**负载均衡不做**,划给网关层(LiteLLM 等),不周山只保证路由 SPI 可对接。

**机制边界(管什么/不管什么)**:
- 管:主备切换、多级降级(贵→便宜)、全故障静态兜底应答;触发 = 韧性层事件驱动(重试耗尽/熔断打开/持续 429),不做独立健康探测
- 生效粒度:**调用级路由**——每次模型调用按熔断状态选链位,熔断恢复(HALF_OPEN 试探成功)自动回主;不做会话粘性(短时抖动不应放大成会话级降级)
- 不管:多实例/多 key 轮询、健康度加权等负载均衡(纯流量工程,与会话状态零耦合,网关层已有成熟实现)

**换模型上下文兼容性:模型档位制**——用户把可互换模型声明为**档位组**,框架只在组内 failover;责任划分 = 用户声明档位 + 框架校验能力 + 预算自动重估:
1. token 预算按新模型重估(TokenEstimator 按模型解析)
2. 校验目标模型能力集(并行工具/思维链等),不支持的能力降级(如并行扇出退化串行)
3. provider 专属内容(thinking 块等)跨 provider 切换时剥离(细则留 Spec)

**接缝**:
- 模型链进**绑定级配置**(SpawnOptions/绑定级声明档位组与主备序),`Buzhou.runtime()` 门面加收模型链的重载;单模型 = 长度 1 的链(向后兼容);四层配置照常覆盖
- 路由执行在 ResilienceAdvisor 内(03 已定执行点),切换/熔断状态变化事件进 observability 事件流
- 降级链同时是成本杠杆:11 成本治理的"超限降级"行为可引用本链

**借鉴**:
- LangChain `with_fallbacks` / ModelFallbackMiddleware(失败含 429 即切备、可串多级、调用级语义)— https://docs.langchain.com/oss/python/langchain/middleware/built-in
- OpenAI Agents SDK ModelProvider 划界 + LiteLLM 网关承担负载均衡(SDK 不造路由的先例)— https://openai.github.io/openai-agents-python/models/ 、https://docs.litellm.ai/docs/tutorials/openai_agents_sdk
- LiteLLM model group(档位组概念)— 同上 LiteLLM 文档
- 与 03 同炉:熔断状态机(Resilience4j 概念)+ 归一化错误分类
