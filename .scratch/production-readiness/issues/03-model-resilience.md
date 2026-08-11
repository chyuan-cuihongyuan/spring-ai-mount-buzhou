# 模型层韧性(重试/熔断/超时/429 应对)

Type: grilling
Status: resolved
Blocked by: 02

## Question

不周山是否/如何提供**模型调用韧性层**?(参考文档一.3/一.4/二.5;业界对照见 01 号票成果)

需回答:
1. **做不做**——重试/退避/熔断/超时/限流应对是框架职责,还是组合 Spring 生态(Spring Retry/Resilience4j)?
2. **机制边界**——管哪些失败(网络错误/429/5xx/内容过滤拒绝/超时),不管什么;区分可重试与不可重试异常的规则归谁定义
3. **接缝**——挂在执行脊柱 HarnessToolCallingManager 还是 Advisor 链?与 Hook 链(beforeModel/afterModel)的关系?韧性事件如何进 observability 事件流?
4. **与 Spring AI 内置 retry 的叠加/避让策略**(依赖 02 号票答案)

答题要求:决策到 做/不做/何时做 + 机制边界(管什么/不管什么) + 与既有机制接缝;能借鉴 LangChain/LangGraph 等成熟开源实现就借鉴(注明项目+机制名+链接),无现成借鉴须给博客/研究支撑,纯自主推演标注 `> 【推演】`。

## Answer

(2026-08-11,grilling 收口,五问全采纳推荐)

**决策:做**——自研轻量**模型韧性层**,不引 Resilience4j 依赖;分 M1/M2 两期。

**机制边界(管什么/不管什么)**:
- **M1**:① 归一化错误分类——限流/鉴权/内容/网络/未知 五类,内置跨 provider 默认分类器 + ProviderErrorClassifier SPI 扩展(默认适配 OpenAI/Anthropic/RestClient 系);② 重试——指数退避+抖动,尊重 429 与 Retry-After;③ 统一超时——模型级 deadline 纳入执行脊柱已有的取消传播
- **M2**(与 04 多模型路由同炉设计):④ 熔断——CLOSED/OPEN/HALF_OPEN 状态机(借 Resilience4j 概念不引依赖;业界无一内建,Spec 需 `> 【推演】` 标注并自建评测);⑤ 兜底响应——错误→受控降级输出
- **不管**:内容拒绝(finishReason=CONTENT_FILTER 静默通道)的治理策略——本层只收录"内容拒绝"分类并保证 afterModel 可观测,治理归 12 内容安全票;绕过会话直调 ChatModel 的旁路不管(Harness 只管流经自己的流量)

**接缝**:
- 执行点:独立 **ResilienceAdvisor**,与 HookAdvisor 相邻挂 ChatClient 链(已查证:会话内模型调用全走此链——DefaultAgentSession 持 ChatClient,Buzhou 门面 runtime()/enhance() 两入口均汇入)
- 配套:Hook 链补 **onModelError** 切面,护栏/观测 Hook 可感知错误、可吞错兜底
- 重试/熔断/超时事件全部进 observability 事件流;策略走 policy 四层配置
- 与底座叠加:语义分工(底座只管网络瞬断,429/5xx/熔断/降级归不周山)+ 数值避让(AutoConfiguration 建议调小底座 max-attempts,写入机制文档)

**借鉴**:
- LangChain v1 ModelRetryMiddleware/ModelFallbackMiddleware(容错下沉为中间件)— https://docs.langchain.com/oss/python/langchain/middleware/built-in
- LangGraph RetryPolicy(声明式重试,默认值含抖动)— https://docs.langchain.com/oss/python/langgraph/fault-tolerance
- Google ADK on_model_error_callback(错误回调吞错兜底/改写控制流)— https://adk.dev/callbacks/
- OpenAI Agents SDK error_handlers(终态错误→受控输出)— https://openai.github.io/openai-agents-python/running_agents/
- Resilience4j 熔断状态机语义(仅借概念)— https://resilience4j.readme.io/docs/getting-started-3
- 底座事实依据:02 票成果(`research/spring-ai-baseline` 分支)
