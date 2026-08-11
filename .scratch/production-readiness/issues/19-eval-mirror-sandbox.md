# 线上评估闭环、流量镜像与沙箱仿真

Type: grilling
Status: resolved

## Question

> **18 已决(2026-08-11)**:会话分叉(fork)已立——生产会话从指定轮次复制历史到新 sessionId 续跑,是本票评估回归/流量镜像的执行手段;重放语义"承诺同起点、不承诺复现"同样适用于本票的镜像与评测结论解释。

框架是否提供**质量验证家族**能力?(参考文档十.4 流量镜像到测试环境复现、十二.2/十二.3 测试框架/仿真批量评测、十二.4 沙箱模式禁止真实接口;ticket 21 已有离线评测套件 demo)

需回答:
1. **做不做**——生产流量采样评估、质量回归、流量镜像、沙箱/mock 模式,哪些归框架
2. **机制边界**——评估触发(采样/事件驱动)与评估器插拔(LLM-judge/规则)框架管到哪;线上评估与离线评测(已有 demo)的边界;流量镜像的语义(复制流量到影子环境 vs 录制后重放);沙箱是部署形态还是运行时开关
3. **接缝**——与 ticket 21 评测套件的关系(扩展它还是新开);评估结果回流 observability/dashboard;镜像/重放与 18 回放的数据通路复用;沙箱与 ToolSetProvider(mock 工具替换)的关系

答题要求:同 08 号票(做/不做 + 机制边界 + 接缝;借鉴成熟实现——LangSmith 线上评估等——并注明来源)。

## Answer

(2026-08-11,grilling 收口,三问全采纳推荐)

**决策:三做一替代**——在线采样评估(做)/ 离线回归(做,扩展 ticket 21)/ 流量镜像(**不做实时影子**,务实替代 = fork 离线双跑)/ 沙箱(做,运行时开关)。

**机制边界(管什么/不管什么)**:
- **在线评估(做)**:规则模型 = **过滤 × 采样 × 评估器引用**(LangSmith run rule 蓝本,配置驱动走 policy 四层);触发 = 观测管线后置异步(不阻塞会话主链路);评估器 SPI(LLM-judge / 代码规则双型);结果回流 observability/dashboard;**工具轨迹匹配作一等评估指标**(ADK 蓝本——Agent 评估不只看最终文本,事件流本有完整工具调用轨迹)
- **离线回归(做)**:评测基建(fixture / 指标断言 / judge 客户端 SPI)从 ticket 21 demo 提炼(test-jar 或独立模块,落点留 Spec),SummaryEvaluationTest 为其首个用例;线上/线下**共享评估器 SPI**;线上 = 无参考答案+采样,离线 = 数据集+参考答案+阈值门禁
- **流量镜像(务实替代)**:采样真实流量 → 18 fork → 离线双跑对比(LangSmith convert_runs_to_test 蓝本);**不做实时影子运行**——业界无一框架内建,且需独立影子部署+流量复制设施,远超 harness 边界
- **沙箱(做)= 绑定级运行时开关**:ToolSetProvider 供给 mock 工具集 + ScriptedChatModel(core testsupport 已有),禁止真实接口;**非部署形态**(harness 是嵌入库,部署形态归用户)
- **不管**:judge prompt 业务实现(judge 与被测场景强相关,内置模板必沦为鸡肋)、评估结论处置(人工评审/CI 门禁策略)、评估平台 UI

**接缝**:
- ticket 21:扩展不新开——评测基建提炼后 SummaryEvaluationTest 迁移为首个用例
- 18:fork 是生产流量转回归集/离线双跑的执行通道;重放语义"承诺同起点、不承诺复现"适用于评测结论解释
- 评估结果作**质量信号**回流 observability(普通事件/指标),**不进 16 治理事件族**(非治理动作)
- 沙箱 mock 工具经 ToolSetProvider SPI 供给,与 MCP 热插拔/14 权限过滤同通道
- 20:评估分/评估器自身故障是监控指标源

**借鉴**:
- LangSmith 在线评估(run rule = filter × sampling_rate × evaluator;在线=生产流量无参考答案,离线=数据集+参考答案 的官方区分)— https://docs.langchain.com/langsmith/evaluation-concepts
- LangSmith 回测(`convert_runs_to_test()`:生产 run 转数据集+基线 experiment,离线回放对比——流量镜像的务实替代)— https://docs.langchain.com/langsmith/run-backtests-new-agent
- Google ADK AgentEvaluator(`tool_trajectory_avg_score` 工具轨迹匹配 = 一等评估指标)— https://github.com/google/adk-python
- Anthropic(LLM-as-judge + 抽样人审结合的评估方法学共识)— https://www.anthropic.com/engineering/multi-agent-research-system
- 业界空白确认:实时流量镜像/影子运行无框架内建;框架层沙箱仿真空白(01 票维度 11)
