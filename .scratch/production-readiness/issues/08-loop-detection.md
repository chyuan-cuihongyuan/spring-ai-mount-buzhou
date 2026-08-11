# 死循环与失控检测(runaway protection)

Type: grilling
Status: resolved

## Question

框架是否提供**死循环与失控检测**?(参考文档二.1:限定最大思考步数/最大工具调用次数、检测重复思考与循环调用、到阈值强制终止并告警)

需回答:
1. **做不做**——单轮内行为失控(死循环、重复调用、步数爆炸)的检测与强制终止是否框架职责
2. **机制边界**——检测维度(最大步数/最大工具调用次数/重复模式识别/单轮 wall-clock 超时);到阈值后的行为(强制终止并返回告警?降级?转人工?);与 11 成本治理(token 硬顶防"花费失控")的分工——行为失控 vs 花费失控
3. **接缝**——检测器挂在执行脊柱(它能看见完整工具调用循环)还是 Hook 链;终止语义与取消传播(执行脊柱已有)的关系;失控事件进 observability;阈值走 policy 四层配置

答题要求:决策到 做/不做/何时做 + 机制边界(管什么/不管什么) + 与既有机制接缝;能借鉴 LangChain/LangGraph 等成熟实现就借鉴(如 LangGraph recursion_limit——注明项目+机制名+链接),无借鉴须给博客/研究支撑,纯自主推演标注 `> 【推演】`。

## Answer

(2026-08-11,grilling 收口,三问全采纳推荐)

**决策:做**——数值硬顶 + 语义检测两层分期;软退出通道是差异化亮点。

**机制边界(管什么/不管什么)**:
- **M1 数值硬顶**:轮内最大步数/最大工具调用次数(**双窗口**:轮次级 × 会话级,可按工具单独限额)+ 单轮 wall-clock 超时;直接对齐业界最小共识
- **M2 语义级重复检测**(业界无一内建,Spec 标 `> 【推演】`+自建评测):收敛为确定性规则——连续 N 次同工具同参数调用、状态原地踏步;**不做**语义相似度判断(避免误杀合法的分页翻读类循环)
- **退出语义三件套**:① **软退出通道**——达软阈值时经 Attachment 通道向模型注入"剩余步数"信号(与事实闭环同构),让模型主动收尾;② 硬顶到点强制终止,受控终态**携带部分结果**(被终止 ≠ 前功尽弃);③ 失控事件进 observability,可配置转人工(与 15 运行时干预联动)
- **不管**:花费失控(token 硬顶归 11);终止条件的可组合代数(AutoGen `&`/`|` 式,过度设计,不做);阈值默认数值留 Spec(safe-by-default 原则给保守默认)

**接缝**:
- 检测器主体在**执行脊柱**(工具循环驱动处,天然看见完整调用序列);Hook 链发失控事件进 observability(与 03 韧性事件、07 限流事件同一通道);阈值走 policy 四层配置
- 软退出信号复用 Hook→state→Attachment 闭环的注入通道
- 转人工联动 15 运行时干预的干预通道

**借鉴**:
- LangGraph recursion_limit + GraphRecursionError(步数硬顶+专用异常+按调用覆盖)— https://docs.langchain.com/oss/python/langgraph/graph-api
- LangGraph RemainingSteps(软退出通道蓝本)— https://docs.langchain.com/oss/python/langgraph/graph-api
- LangChain v1 ModelCallLimitMiddleware/ToolCallLimitMiddleware(双窗口限额+按工具粒度)— https://docs.langchain.com/oss/python/langchain/middleware/built-in
- OpenAI Agents SDK max_turns + MaxTurnsExceeded(异常携带部分结果)— https://openai.github.io/openai-agents-python/running_agents/
- AutoGen Termination Conditions(可组合代数——评估后不做,记录于此)— https://microsoft.github.io/autogen/stable/user-guide/agentchat-user-guide/tutorial/termination.html
