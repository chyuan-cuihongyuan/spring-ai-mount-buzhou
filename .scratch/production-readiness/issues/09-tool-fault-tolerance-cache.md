# 工具调用容错与结果缓存

Type: grilling
Status: resolved

## Question

框架是否提供**工具级容错与工具结果缓存**?(参考文档二.5:工具异常捕获/返回清洗/超时兜底/禁用异常工具;五.4 工具熔断降级;五.6 相同参数结果缓存;十三.3 高频问题缓存)

需回答:
1. **做不做**——工具异常清洗、超时兜底返回、工具级熔断/降级、异常工具自动禁用、幂等工具的结果缓存,哪些归框架
2. **机制边界**——与 03 模型层韧性的分工(模型调用韧性 vs 工具调用韧性);缓存的适用范围(只有无副作用工具可缓存?TTL 归谁定);"高频问题缓存绕过 LLM"(参考文档十三.3)是框架职责还是业务职责
3. **接缝**——挂在执行脊柱工具 fan-out 处还是 afterTool Hook;熔断/禁用状态存哪(会话 state?实例级?);与 MCP 热插拔(工具集动态刷新)的协同;缓存存储复用哪个 SPI

答题要求:同 08 号票(做/不做 + 机制边界 + 接缝;借鉴成熟实现并注明来源)。

## Answer

(2026-08-11,grilling 收口,三问全采纳推荐)

**决策:做**——工具容错分期 + 声明式结果缓存;"高频问题缓存绕过 LLM"不做。

**机制边界(管什么/不管什么)**:
- **M1 按工具策略的异常处理**:工具重试(可配次数/退避)+ 异常清洗与兜底返回——把底座全局布尔开关(`throw-exception-on-error`)升级为**按工具策略**:回注重试/兜底文案/直接抛出,按工具配置
- **M2 工具熔断与自动禁用**:连续失败→熔断该工具一段时间;复用 03 熔断状态机概念,作用域=工具(03 管模型调用、本票管工具调用,状态机同族不同实例)
- **声明式结果缓存**(借 LangGraph CachePolicy:键函数+TTL):**前提=工具声明幂等**(05 三件套延伸——声明幂等即"可缓存候选");键=工具名+参数规范化(键函数可覆盖);默认内存实现+SPI 可扩展。与 05 去重表的区别:去重表防"重放重复执行"(正确性),缓存省"重复计算/调用"(效率)
- **不管**:高频问题应答缓存(绕过 LLM 的语义缓存——业务层/网关层题材,非 Harness);工具内部业务正确性

**接缝**:
- **脊柱管执行、Hook 管策略**:重试/超时在执行脊柱(含超时预算重新计时);缓存与兜底走 Hook 链——beforeTool 查缓存(命中短路返回),afterTool 写缓存+异常清洗改写(HookedToolCallback 切面已支持短路语义)
- 熔断状态实例级;策略走 policy 四层配置(工具级覆盖天然契合);事件进 observability
- MCP 热插拔协同:工具下线时其缓存条目与熔断状态随引用计数回收(热插拔已有差量刷新+引用计数延迟关闭)

**借鉴**:
- LangChain v1 ToolRetryMiddleware(工具重试中间件)— https://docs.langchain.com/oss/python/langchain/middleware/built-in
- LangGraph CachePolicy + BaseCache(声明式缓存:键函数+TTL)— https://docs.langchain.com/oss/python/langgraph/use-functional-api
- Google ADK on_tool_error_callback(错误回调改写返回=兜底)— https://adk.dev/plugins/
- CrewAI guardrail(输出校验失败→带原因重试)— https://docs.crewai.com/concepts/tasks
- 底座事实依据:02 票成果 §6(全局布尔、无按工具策略)
