# 借鉴与引用清单

> 《生产就绪缺口盘点与取舍路线图》的配套清单（2026-08-11）。
> 原则：能借鉴成熟开源项目的治理实现就借鉴，不重新发明；无现成借鉴时须有博客/研究支撑；纯自主推演单独标注。
> 逐缺口的完整决策见 [README.md](README.md) 与 `.scratch/production-readiness/issues/` 各票 Answer。

## 一、按缺口的借鉴明细

### M1 稳定基线

**03 模型层韧性（重试/熔断/超时/429）**
- LangChain v1 中间件：RetryModelMiddleware / ModelFallbackMiddleware / ModelCallLimitMiddleware — https://docs.langchain.com/oss/python/releases/langchain-v1
- LangChain `with_fallbacks`（链式兜底形态）— https://python.langchain.com/docs/how_to/fallbacks/
- LangGraph RetryPolicy（节点级重试策略）— https://docs.langchain.com/oss/python/langgraph/
- Resilience4j 熔断状态机（仅借概念，不引依赖）— https://resilience4j.readme.io/
- LangChain InMemoryRateLimiter（单实例桶形态）— https://python.langchain.com/api_reference/core/rate_limiters/

**05 崩溃恢复 + 幂等**
- LangGraph durability 三档（sync/async/exit）— https://docs.langchain.com/oss/python/langgraph/durable-execution（另见 checkpointer 契约测试包 — https://github.com/langchain-ai/langgraph）
- Anthropic 工程博客《Scaling Managed Agents》（wake(sessionId) 从追加日志重建）— https://www.anthropic.com/engineering/managed-agents
- OpenAI Agents SDK RunState 序列化外迁 — https://openai.github.io/openai-agents-python/
- 幂等三件套（声明/幂等键/去重）：**业界空白，`> 【推演】`**

**06 优雅停机与 drain**
- Spring Boot 4 graceful shutdown（只管 web 请求——底座留白盘点结论，见 02 票研究）
- LangGraph Platform 线程存活语义（会话跨实例续接天然性的参照）

**07 背压与多层限流**
- LangChain InMemoryRateLimiter（每模型 RPM/TPM 双桶形态）— https://python.langchain.com/api_reference/core/rate_limiters/
- CrewAI `max_rpm`（每 Agent 速率声明先例）— https://github.com/crewAIInc/crewAI
- LiteLLM model group / 网关预算（网关层职责划界的对接面）— https://docs.litellm.ai/

**08 死循环与失控检测**
- LangGraph `recursion_limit` / RemainingSteps（数值硬顶 + 剩余步数注入模型）— https://docs.langchain.com/oss/python/langgraph/
- AutoGen Termination 代数 / TokenUsageTermination（终止条件形式化参照）— https://microsoft.github.io/autogen/stable/reference/python/autogen_agentchat.conditions.html
- M2 确定性重复检测（同工具同参数连续重复）：**业界空白，`> 【推演】`**（语义相似度检测明确不做）

### M2 可控治理

**04 多模型路由/降级链**
- LangChain v1 ModelFallbackMiddleware / `with_fallbacks`（降级链形态）— https://docs.langchain.com/oss/python/releases/langchain-v1
- LiteLLM model group（模型分组与网关路由——负载均衡划网关的依据）— https://docs.litellm.ai/
- 模型档位制、调用级路由：`> 【推演】`（在 fallback 蓝本上的会话模型适配）

**09 工具容错与结果缓存**
- LangGraph CachePolicy（声明式缓存：键函数 + TTL）— https://docs.langchain.com/oss/python/langgraph/
- CrewAI guardrail 带原因重试（工具侧校验重试形态）— https://github.com/crewAIInc/crewAI
- OpenAI Agents SDK error_handlers（工具异常处理挂载点）— https://github.com/openai/openai-agents-python

**11 成本治理**
- LangSmith 双通道计量（SDK 自动上报 + 手工上报）与定价表 — https://docs.langchain.com/langsmith/
- AutoGen TokenUsageTermination（token 硬顶终止先例）— https://microsoft.github.io/autogen/stable/reference/python/autogen_agentchat.conditions.html
- 预算硬顶 + 三级动作（告警→降级链→阻断）：**业界空白，`> 【推演】`**

**12 内容安全护栏**
- LangChain v1 中间件体系（挂载点与动作语义）— https://docs.langchain.com/oss/python/releases/langchain-v1
- OpenAI Agents SDK guardrail + tripwire（护栏阻断形态）— https://openai.github.io/openai-agents-python/guardrails/
- CrewAI guardrail 带原因重试 — https://github.com/crewAIInc/crewAI
- 外部检测器依赖声明：Lakera — https://www.lakera.ai/ ；LLM Guard — https://github.com/protectai/llm-guard

**13 PII 脱敏与数据加密**
- LangChain v1 PIIMiddleware（检测器可插/动作可配/作用点可选）— https://docs.langchain.com/oss/python/releases/langchain-v1
- Microsoft Presidio（假名化/可逆映射的业界成熟做法）— https://microsoft.github.io/presidio/
- OpenAI Agents SDK EncryptedSession（存储加密 = Session 装饰器）— https://github.com/openai/openai-agents-python

**14 工具权限模型**
- OpenAI Agents SDK needs_approval + 工具过滤（审批+过滤体系化先例）— https://openai.github.io/openai-agents-python/human_in_the_loop/
- 主体×工具组矩阵（RBAC 最简形）：**业界空白，`> 【推演】`**；SPI 对接面参照 OPA — https://www.openpolicyagent.org/

**15 运行时干预**
- LangChain v1 HumanInTheLoopMiddleware（approve/edit/reject/respond 四决策；edit = 纠偏注入官方形态）— https://docs.langchain.com/oss/python/langchain/human-in-the-loop
- OpenAI Agents SDK RunState 外迁 + approve/reject 续跑（等人工不占进程）— https://openai.github.io/openai-agents-python/human_in_the_loop/
- Google ADK LongRunningFunctionTool（长任务挂起-回填）— https://github.com/google/adk-python
- AutoGen ExternalTermination（kill switch 官方形态）— https://microsoft.github.io/autogen/stable/reference/python/autogen_agentchat.conditions.html
- LangGraph `interrupt()`（断点 + 重跑语义）— https://docs.langchain.com/oss/python/langgraph/interrupts
- HumanLayer《12-Factor Agents》Factor 6/7（等待人类 = 未返回的工具调用）— https://github.com/humanlayer/12-factor-agents

**16 审计证据链**
- LangSmith「Trace 即证据」（观测与审计共用一份数据）— https://docs.langchain.com/langsmith
- Google ADK Events（事件溯源 = 会话存储官方数据模型）— https://github.com/google/adk-python
- Anthropic 工程博客《Scaling Managed Agents》（Session = append-only event log）— https://www.anthropic.com/engineering/managed-agents
- OpenAI Agents SDK tracing 默认开（opt-out）— https://github.com/openai/openai-agents-python
- LangGraph `get_state_history`（checkpoint 历史 = 状态迁移日志）— https://github.com/langchain-ai/langgraph
- 防篡改（哈希链/签名/合规留存格式）：**业界空白**——L2 对接外部 WORM，框架内哈希链仅作可选增强留 Spec `> 【推演】`

**21 多租户**
- LangSmith Engine security（多租户 = 应用层逻辑隔离的官方明示；org/workspace 级 RBAC）— https://docs.langchain.com/langsmith/engine-security
- CrewAI memory scope（记忆作用域命名空间隔离形态）— https://github.com/crewAIInc/crewAI
- 隔离强度定级（命名空间级逻辑隔离）：`> 【推演】`（按参照系定级）；强隔离（行级安全/按租户密钥）业界空白，不做

### M3 可解释与运营

**17 Planning 机制**
- LangChain v1 TodoListMiddleware / DeepAgents（计划自报 = 结构化 todo 工具 + 渲染进上下文）— https://docs.langchain.com/oss/python/langchain/middleware/built-in
- Google ADK BasePlanner / PlanReActPlanner（先计划后行动 = prompt 级约束蓝本）— https://github.com/google/adk-python
- CrewAI `planning_llm`（规划/执行成本分层——单 Agent 不适用，记录划走）— https://github.com/crewAIInc/crewAI
- Anthropic《Building effective agents》（orchestrator-workers 模式——多 Agent 出图）— https://www.anthropic.com/engineering/building-effective-agents

**18 会话回放与调试**
- LangGraph Time Travel（`get_state_history` / replay / fork 语义——业界最完整时间旅行规范）— https://docs.langchain.com/oss/python/langgraph/use-time-travel
- LangSmith 生产 trace 转数据集回放 — https://docs.langchain.com/langsmith/run-backtests-new-agent
- LangGraph Studio（调试 UI 与运行时同源）— https://github.com/langchain-ai/langgraph
- fork 落点形态（会话模型 vs 图模型）：`> 【推演】`；业界共识：时间旅行是 LangGraph 独有一等功能，OpenAI/AutoGen/CrewAI 空白

**19 评估/镜像/沙箱**
- LangSmith 在线评估 run rule（filter × sampling_rate × evaluator；在线 vs 离线官方区分）— https://docs.langchain.com/langsmith/evaluation-concepts
- LangSmith 回测 `convert_runs_to_test()`（流量镜像的务实替代）— https://docs.langchain.com/langsmith/run-backtests-new-agent
- Google ADK AgentEvaluator（`tool_trajectory_avg_score` 工具轨迹 = 一等评估指标）— https://github.com/google/adk-python
- Anthropic《How we built our multi-agent research system》（LLM-judge + 抽样人审方法学）— https://www.anthropic.com/engineering/multi-agent-research-system
- 实时流量镜像/影子运行、框架层沙箱仿真：业界空白，不做（镜像走 fork 双跑替代）

**20 运营监控与性能基线**
- LangSmith 自托管 Prometheus 指标（暴露标准端点接既有监控栈）— https://docs.langchain.com/langsmith/self-hosted-changelog
- LangSmith dashboards/alerts/webhooks 三件套（平台形态参照，嵌入库不照搬告警）— https://docs.langchain.com/langsmith
- OpenTelemetry GenAI Semantic Conventions（`gen_ai.*` 属性命名）— https://opentelemetry.io/docs/specs/semconv/gen-ai/
- AutoGen 原生 OTel 埋点（框架内建插桩标配化）— https://github.com/microsoft/autogen
- Micrometer / Prometheus / Alertmanager 生态（告警通道不重新发明）

**22 会话数据生命周期**
- LangGraph Thread TTL / Store TTL（`default_ttl` + 超期策略 + 周期清扫；`refresh_on_read` 读时续期；`put(ttl=...)` 条目级覆盖；`start_ttl_sweeper`）— https://github.com/langchain-ai/langgraph
- LangSmith 留存双档 + 按 workspace 自定义 + 数据集豁免（留存分档 + 例外清单范本）；数据清除 API + 删除语义分级 — https://docs.langchain.com/langsmith/usage-and-billing 、 https://docs.langchain.com/langsmith/data-purging-compliance
- CrewAI `reset-memories`（记忆运维清理 CLI 形态）— https://github.com/crewAIInc/crewAI

**23 Prompt/配置版本治理**
- LangSmith Prompt Hub（`pull_prompt("name:commit")` 钉住不可变版本——prompt 即内容寻址制品）— https://docs.langchain.com/langsmith/manage-prompts-programmatically
- LangGraph Assistants（配置与代码分离的版本化）— https://github.com/langchain-ai/langgraph
- LangGraph 官方 Backward Compatibility（`flow_version` 模式：版本路由、在跑线程不受发版影响——灰度的状态机解法）— https://docs.langchain.com/oss/python/langgraph/backward-compatibility
- 按流量百分比 canary：业界空白，不做

**24 RAG 检索治理（不做）**
- 划界依据：检索新鲜度/ACL 过滤/引用强制/索引生命周期，主流框架均无内建，由向量库/搜索层承担（维度 15 研究结论）
- langgraph-bigtool（工具集超规模时的语义检索选择——ToolSetProvider 候选，留 Spec）— https://github.com/langchain-ai/langgraph-bigtool
- LangSmith RAG 评估器（检索质量纳入 19 评估闭环）— https://docs.langchain.com/langsmith/evaluation-concepts

**10 Agent 实例池/调度器（基本不做）**
- 划界依据：Anthropic Managed Agents 架构（容器化托管运行时是平台层职责）— https://www.anthropic.com/engineering/managed-agents；Google ADK LongRunningFunctionTool（长任务挂起-回填由 15 吸收）— https://github.com/google/adk-python

## 二、底座研究（本 effort 自产）

- 01 业界成熟方案对标（16 维度：12 项共识可直接借、12 项空白需推演）— `.scratch/production-readiness/research/01-industry-benchmark.md`（`research/industry-benchmark` 分支）
- 02 Spring AI 2.0 / Spring Boot 4 内置能力盘点（8 条底座留白 = 不周山合法空间）— `.scratch/production-readiness/research/02-spring-ai-baseline.md`（`research/spring-ai-baseline` 分支）

## 三、纯推演项汇总（无借鉴、自主设计，Spec 阶段须标 `> 【推演】`）

| 推演项 | 所在缺口 | 一句话 |
|---|---|---|
| 幂等三件套 | 05 | 副作用工具调用的声明/幂等键（会话+轮次+调用序号）/去重表 |
| 确定性重复检测 | 08 M2 | 同工具同参数连续重复的确定性判定（不做语义相似度） |
| 预算硬顶 + 三级动作 | 11 | 会话/绑定级预算的告警→降级链→阻断 |
| 工具权限矩阵 | 14 | 主体×工具组 allow/deny 矩阵（RBAC 最简形） |
| 框架内哈希链（可选增强） | 16 | 审计事件前序哈希链；默认走外部 WORM 对接 |
| fork 落点形态 | 18 | LangGraph 图模型 fork 语义 → 不周山会话模型的适配 |
| 隔离强度定级 | 21 | 参照系（私有化+企业内部）下取命名空间级逻辑隔离 |
| 模型档位制 / 调用级路由 | 04 | 用户声明档位组、框架校验能力、熔断恢复自动回主 |

## 四、研究/博客引用汇总

- Anthropic 工程博客《Scaling Managed Agents》— https://www.anthropic.com/engineering/managed-agents
- Anthropic《Building effective agents》— https://www.anthropic.com/engineering/building-effective-agents
- Anthropic《How we built our multi-agent research system》— https://www.anthropic.com/engineering/multi-agent-research-system
- HumanLayer《12-Factor Agents》— https://github.com/humanlayer/12-factor-agents
- OpenTelemetry GenAI Semantic Conventions — https://opentelemetry.io/docs/specs/semconv/gen-ai/
- 用户桌面功能清单（仅参考，非需求清单）— `.scratch/production-readiness/reference/buzhou-feature-list.md`
