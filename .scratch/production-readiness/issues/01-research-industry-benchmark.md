# 业界成熟方案对标(LangChain/LangGraph 为主)

Type: research
Status: resolved

## Question

对标业内认同度高的成熟开源 Agent 框架/平台,盘点它们在生产治理各维度的**官方机制**,作为不周山缺口决策的借鉴水源。

**重点对象**:LangChain / LangGraph(含 LangGraph Platform 的 checkpointer、interrupt/HITL、time travel、store)、LangSmith(评估/监控)。
**次要对象**:OpenAI Agents SDK、Google ADK、AutoGen、CrewAI;以及高影响力博客/研究(如 HumanLayer "12-Factor Agents"、Anthropic 工程博客等)。

**按以下维度输出对照矩阵**(每个维度:官方机制名 + 设计要点 + 可借鉴点 + 一手来源链接):

1. 模型容错(重试/熔断/超时/429 应对)与多模型路由(负载均衡/主备/降级链)
2. 会话持久化、崩溃恢复(检查点/in-flight 恢复)、幂等
3. 失控防护:死循环检测、最大步数、token 硬顶、背压限流
4. 工具治理:容错、熔断、结果缓存、权限/RBAC
5. 成本治理:预算/配额/计量归因
6. 内容安全(注入检测/输出审核)与 PII 脱敏
7. 运行时干预:暂停/恢复/人工接管/纠偏注入/kill switch
8. 审计证据链
9. Planning(计划自报/审批)
10. 会话回放/时间旅行/调试模式
11. 线上评估/流量镜像/沙箱仿真
12. 运营监控与告警
13. 多租户与数据生命周期(TTL/归档/删除)
14. Prompt/配置版本治理与灰度
15. RAG 检索治理
16. Agent 实例生命周期与异步任务调度

结论部分指出:哪些维度业界已有**共识做法**(直接可借),哪些维度各家分歧或空白(需自主推演)。

产出写入本文件同目录的 `../research/01-industry-benchmark.md`,中文,所有关键论断附一手链接(官方文档/GitHub)。

## Answer

(2026-08-11,研究子代理 AFK 收口)

**要点**:16 维度对标完成(LangGraph/LangChain/LangSmith 重点 + OpenAI Agents SDK/Google ADK/AutoGen/CrewAI + 12-Factor Agents/Anthropic 工程博客),全部一手来源、关键数值查到源码级出处。总览矩阵以 ●/◐/○ 标注各家官方机制成熟度。结论两层:**12 项业界共识可直接借**(durability 三档、双窗口限额中间件、HITL 四决策、TTL+refresh_on_read、在线评估"过滤×采样×评估器"规则模型、prompt commit 钉版、checkpointer 契约测试包等,多数可直接翻译成不周山机制);**12 项空白/分歧需自主推演**(熔断器、语义死循环检测、业务幂等、工具 RBAC、预算硬顶、注入检测、审计防篡改、影子流量、百分比灰度、强隔离多租户、RAG 治理、计划审批流)——与本地图后续票高度重合,佐证专题必要性。

**成果**:分支 `research/industry-benchmark`(commit 4ecde5a)→ `.scratch/production-readiness/research/01-industry-benchmark.md`。

**用法**:03–24 各 grilling 票开会时,先 zoom 本成果的对应维度章节,把"可借鉴点"作为决策起点。
