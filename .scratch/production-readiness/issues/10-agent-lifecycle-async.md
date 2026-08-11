# Agent 实例生命周期与异步任务调度(边界拷问)

Type: grilling
Status: resolved

## Question

框架要不要管理**Agent 实例与异步任务的生命周期**?(参考文档一.1:实例池/热加载/动态启停/实例隔离;一.5:异步排队/任务优先级/取消/延迟执行/定时任务)

需回答:
1. **做不做**——Agent 实例池(热加载、动态启停、资源隔离)、异步任务调度(排队/优先级/定时)是框架职责,还是留给用户业务层/基础设施(如 Spring 的 TaskExecutor、 Quartz)?这是边界拷问,答案可以是"只提供 SPI 钩子"
2. **机制边界**——若做,"Agent 实例"在不周山的语义是什么(现在只有会话+租约,没有实例概念);实例隔离做到什么程度(线程/内存/配置)
3. **接缝**——与 AgentRuntime.spawn()/AgentSession/租约的关系;异步任务与会话模型的关系(任务是会话的载体还是会话外实体);与 07 背压(排队语义)的分工

答题要求:同 08 号票(做/不做 + 机制边界 + 接缝;借鉴成熟实现并注明来源)。

## Answer

(2026-08-11,grilling 收口,三问全采纳推荐)

**决策:基本不做**——实例池与调度器都不做;只做钩子与模式文档;长任务挂起-回填原语归 15 同炉。

**机制边界(管什么/不管什么)**:
- **不做 Agent 实例池**:不周山的"Agent"是配置绑定(模型链+工具集+Skill+策略),不是运行实例。参考文档一.1 诉求的吸收对照:热加载=MCP 热插拔/Skill DB 覆盖/四层配置;实例隔离=会话边界(无跨会话共享内存);销毁回收=会话 close + 租约释放;多业务隔离=多绑定共存。引入"实例"会造出与会话平行的第二生命周期(两套生命周期是混乱之源)。业界同判:无一家有实例池概念(LangGraph Platform 是 assistants 配置版本 + threads 会话二分)
- **不做调度器**:排队/优先级/定时/cron 全交 Spring 生态(TaskExecutor/@Scheduled/Quartz/消息队列);LangGraph Platform 的 cron 是其作为托管平台的职责,不周山是库,调度归部署方
- **做钩子与模式**:① 会话句柄异步语义补强(可取消/状态查询,Spec 期核实现状缺口)② 文档化"异步任务模式":队列+spawn+05 的 opt-in 自动重驱动(无人值守场景闭环)
- **长任务挂起-回填原语**:方向认可,归 15 运行时干预同炉——与 HITL 等待本质同构(都是"挂起等外部事件回填"),分开设计会造两套等待机制

**借鉴**:
- LangGraph Platform assistants+threads 二分(无实例池的同判)— https://github.com/langchain-ai/langgraph
- LangGraph Platform cron × webhook(托管平台职责的划界参照)— 同上
- Google ADK LongRunningFunctionTool(挂起-回填原语蓝本,归 15)— https://github.com/google/adk-python
- Anthropic Managed Agents 三层解耦(brain/hands/session,harness 无状态化)— https://www.anthropic.com/engineering/managed-agents
