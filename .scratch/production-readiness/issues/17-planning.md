# Planning 机制(计划自报/审批/可改)

Type: grilling
Status: resolved

## Question

是否引入**规划机制**——Agent 自报计划、计划可审可改?(第一轮 Q3 已决:planning 作为单 Agent 能力边界内的候选缺口留在图内;参考文档无直接对应,业界对照:LangGraph plan-and-execute 模式、各 Agent 框架的 todo/planning 工具)

需回答:
1. **做不做**——显式规划(计划作为一等对象)是框架职责,还是留给 prompt/工具层
2. **机制边界**——规划形态:显式计划对象(结构化计划进 state)vs 原子工具任务清单的增强;计划与执行的耦合度(强约束按计划走 vs 参考性计划);计划审批是否作为一种 HITL 门禁
3. **接缝**——与原子工具任务清单(已有)的关系;与事实闭环(计划进度写 state、渲染 Attachment 注入?)的关系;与 15 运行时干预(人类改计划)的协同

答题要求:同 08 号票(做/不做 + 机制边界 + 接缝;借鉴成熟实现——plan-and-execute、Claude Code todo 模式等——并注明来源)。

## Answer

(2026-08-11,grilling 收口,三问全采纳推荐)

**决策:部分做**——计划自报形态**已落地**(TodoTool),本票确认边界 + 补两个模式文档;不建计划一等对象。

**机制边界(管什么/不管什么)**:
- **计划自报 = TodoTool**(已存在:会话作用域任务清单,list/upsert/remove/clear,入 SessionStateStore 持久化,TodoAttachmentRenderer 下轮注入)——与 LangChain TodoListMiddleware 完全同构,官方形态确认
- **软约束(参考性计划)**:计划是模型自报的工作清单,框架**不强制按计划走**;强约束需计划-执行分离架构(LangGraph plan-and-execute 双角色),出单 Agent 边界、过度工程
- **计划审批 = 机制可拼装**:todo 工具挂 14 HITL 门禁即得(模型一次完整 upsert 写出全计划 → 拦截审批 = 审批整个计划);15 挂起-回填使审批等待可外迁不占线程;**不建专门审批机制**(与业界一致:计划审批无框架内建,皆靠 interrupt 拼装)
- **两个模式文档**(归 examples/docs,同 10 异步任务模式文档):① 计划审批拼装法(HITL 门禁配置 + 挂起式审批)② 先计划后行动 prompt 约束(ADK PlanReActPlanner 蓝本,绑定级 prompt 模式,非机制)
- **不管/不做**:计划一等对象(结构化计划的生命周期管理)、Planner SPI(ADK BasePlanner 形态)、plan-and-execute 双角色架构、规划用便宜模型(CrewAI planning_llm——单 Agent 无独立规划 LLM,不适用,记录划走理由)

**接缝**:
- 计划进度已走 Hook→state→Attachment 闭环(现状),无需新增
- 人类改计划 = 15 纠偏注入(operator 消息指示模型改,或直改 SessionStateStore 的 todo 数据)
- 08 联动(计划步骤数作"剩余步数"软信号的分母)留 Spec 评估,本票不承诺
- 计划变更/审批留痕 = 治理事件族(16 四元组)自然覆盖

**借鉴**:
- LangChain v1 TodoListMiddleware / DeepAgents(计划自报 = 结构化 todo 工具 + 渲染进上下文,官方中间件形态)— https://docs.langchain.com/oss/python/langchain/middleware/built-in
- Google ADK BasePlanner / PlanReActPlanner(约束"先出计划再行动"= prompt 级约束蓝本;Planner SPI 形态记录但不采纳)— https://github.com/google/adk-python
- CrewAI planning=True + planning_llm(规划/执行成本分层——单 Agent 无独立规划 LLM,不适用)— https://github.com/crewAIInc/crewAI
- Anthropic《Building effective agents》(orchestrator-workers 计划-执行分离模式——多 Agent 编排,出图)— https://www.anthropic.com/engineering/building-effective-agents
- 业界分歧点确认:计划**审批**无一框架内建,靠 interrupt 机制在计划工具调用处挂审批("机制可拼装、无开箱功能",01 票维度 9)
