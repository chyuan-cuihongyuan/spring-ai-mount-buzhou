# 成本治理(token 硬顶/预算/配额/计费归因)

Type: grilling
Status: resolved

## Question

> **04 已决(2026-08-11)**:模型降级链(贵→便宜)归 04 多模型路由;本票的"超限降级"行为可直接引用该链,不重复造。
> **07 已决(2026-08-11)**:瞬时速率限流(RPM/TPM、并发上限)归 07;本票管**累计量闸门**(预算/配额),二者分工写清。

框架是否提供**成本治理**?(参考文档二.2 token 硬限流、十.3 用量统计/计费/配额拦截。已查证:observability 层已采集 token usage——数据已备,缺闸门)

需回答:
1. **做不做**——token 硬顶(输入/输出/会话累计)、成本预算(按会话/天/租户)、超限行为(阻断/降级/告警)、计费归因,哪些归框架
2. **机制边界**——计量(已有)与闸门(缺)各管什么;归因粒度(会话/用户/租户/部门);成本预算与动态预算的区分——后者管"上下文窗口怎么分",前者管"钱花多少",两者别混
3. **接缝**——闸门挂在 Hook 链哪环(beforeModel 预检?afterModel 累计?);usage 数据从 observability 哪个环节取;配额状态存哪(哪个 SPI);超限事件进 observability;与 21 多租户(按租户配额)的协同

答题要求:同 08 号票(做/不做 + 机制边界 + 接缝;借鉴成熟实现并注明来源)。

## Answer

(2026-08-11,grilling 收口,三问全采纳推荐)

**决策:做**——计量→定价→闸门全链;预算硬顶是业界空白,Spec 标 `> 【推演】`+自建评测。

**机制边界(管什么/不管什么)**:
- **双通道计量**(借 LangSmith):usage 自动换算(内置主流模型定价表,配置可覆盖,不承诺实时跟随厂商定价)+ 任意成本手工上报通道(工具调用成本可入)
- **预算作用域 M1**:会话级 + 绑定级(appId+agentName)两档;归因三级标签(会话/绑定/单次调用);租户维度归 21(本票留归因扩展位);按周期滚动配额(日/月)为 M2、与 21 同炉
- **超限三级动作**(可配):软阈值**告警**(事件进 observability)→ 中间档**降级**(引用 04 降级链切便宜模型——降级链第二个消费场景)→ 硬顶**阻断**(受控终态,可转人工);软阈值经 **Attachment 注入"预算余量"信号**让模型主动省用(08 软退出通道同构延伸)
- **不管**:瞬时速率(07 的 RPM/TPM);租户配额(21);计费出账(框架给归因数据,出账是业务系统的事)

**接缝**:
- 闸门挂 beforeModel 预检(Hook 链),计量在 afterModel 累计(usage 数据流已有);累计状态进会话 state
- 与 07 分工写清:07 管瞬时速率,本票管累计闸门;同属失控防护家族(行为失控 08/速率失控 07/花费失控 11)
- 策略走 policy 四层配置

**借鉴**:
- LangSmith Cost Tracking(token×定价表自动算 + 手工上报双通道)— https://docs.langchain.com/langsmith/cost-tracking
- AutoGen TokenUsageTermination(预算即终止条件,业界最接近者)— https://microsoft.github.io/autogen/stable/reference/python/autogen_agentchat.conditions.html
- LiteLLM Proxy per-key 预算(网关层做法——评估后不作为本框架方案,记录于此)— https://docs.litellm.ai/docs/tutorials/openai_agents_sdk
