# 审计证据链

Type: grilling
Status: resolved

## Question

框架是否提供**不可篡改的审计证据链**?(参考文档八.1 日志持久化的审计侧;HITL 授权已有 state 标记,缺"谁在何时授权了什么"的证据化)

需回答:
1. **做不做**——危险操作与 HITL 授权的审计是否框架职责;审计范围(授权事件/工具调用/配置变更/运行时干预)
2. **机制边界**——不可篡改做到什么程度:追加写存储?哈希链?还是对接外部 WORM/日志服务(不重新发明)?
3. **接缝**——审计是独立存储还是 observability 事件流的特化视图;与 ObservabilityStore SPI 的关系;与 HITL 授权 state 标记的证据化;与 15 运行时干预留痕的共用通道

答题要求:同 08 号票(做/不做 + 机制边界 + 接缝;借鉴成熟实现并注明来源)。

## Answer

(2026-08-11,grilling 收口,三问全采纳推荐)

**决策:做**——审计 = observability 事件流的**特化视图**,不建独立审计存储(业界共识:观测与审计共用一份数据)。

**机制边界(管什么/不管什么)**:
- **治理事件族规范化**:HITL 授权/拒绝/撤销(已有雏形 `guard.auth.granted/revoked`)、14 权限判定放行/拦截、15 干预操作(暂停/纠偏/kill/接管)、12-13 护栏命中与脱敏动作、11 预算三级动作——统一 **actor/action/target/outcome 四元组**;actor = 14 已决的主体上下文(调用方声明,框架全链贯穿,不认证)
- **审计查询 = 事件流按类型过滤的视图**(dashboard/导出),不是独立数据模型
- **L1 追加写(做)**:SPI 契约声明审计事件**不可更新、不可删**(append-only 语义进 store 契约测试)
- **L2 防篡改(对接,不重新发明)**:框架内不做哈希链/签名(业界空白+非差异化);经导出 SPI/otel 桥对接外部 WORM/日志服务;框架内哈希链作可选增强留 Spec 标 `> 【推演】`
- **L3 留存期/合规格式**:归 22 数据生命周期
- **落库强度独立于 durability 三档**:审计事件始终**同步落库、不采样**(观测事件保持异步可采样);失败**不阻断**主链路(韧性优先,同 GuardAuthApi 现状)但 ERROR 日志+指标+告警钩子(20 联动)
- **不管**:WORM 存储本身、合规认证(等保/SOC2 举证模板)、审计查询 UI

**接缝**:
- 复用 **ObservabilityStore SPI**(不新增第六 SPI);审计事件与 Span/Event 同模型,类型前缀区分
- HITL state 标记证据化:授权六字段答"授权了什么",本票补"**谁**授权"——actor 贯穿是 14 主体上下文声明制的落点之一
- 15 干预留痕共用本通道(干预事件 = 治理事件族成员,同一四元组)
- 观测管线:审计事件走同步旁路(异步管线优先 flush 或独立同步路径,细则留 Spec);06 优雅停机排空清单覆盖审计旁路
- 20:审计落库失败是告警源;22:审计事件留存期与删除豁免(治理事件不随会话数据生命周期删除,细则归 22)

**借鉴**:
- LangSmith(Trace 即证据:全量输入/输出/中间步骤不可变留存,观测与审计共用一份数据)— https://docs.langchain.com/langsmith
- Google ADK(事件溯源:会话由 append-only Event 构成的官方数据模型)— https://github.com/google/adk-python
- Anthropic Managed Agents("Session = append-only event log",大厂生产实践背书)— https://www.anthropic.com/engineering/managed-agents
- OpenAI Agents SDK(tracing 默认开 opt-out:审计级留痕应默认存在)— https://github.com/openai/openai-agents-python
- LangGraph `get_state_history`(checkpoint 历史 = 状态迁移日志)— https://github.com/langchain-ai/langgraph
- 业界空白确认:防篡改/可验证(哈希链、签名、合规留存格式)无一框架内建(01 票维度 8)——故 L2 走外部对接,不自研
