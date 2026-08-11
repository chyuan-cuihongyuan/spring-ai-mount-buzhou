# 会话回放、异常快照与调试模式

Type: grilling
Status: resolved

## Question

框架是否提供**会话回放与调试**?(参考文档 3.2:提示词快照/回放完整执行流程/异常时自动保存上下文快照;3.4:会话 ID 一键回放、断点调试、手动改工具返回)

需回答:
1. **做不做**——基于持久化消息+事件的历史会话 replay、异常自动快照、调试模式(断点/改工具返回)是否框架职责
2. **机制边界**——只读时间旅行(查看任意时刻的完整上下文与证据)vs 分叉重跑(从某点换参重放);重放的语义:模型不确定性下"复现"到底承诺什么
3. **接缝**——数据完备性:MessageStore/ObservabilityStore 现有数据够不够支撑回放,缺什么(prompt 快照?模型参数快照?);与 dashboard 的呈现关系;与 19 评估(回放喂评估)的复用;异常快照与 08 失控检测(触发快照)的联动

答题要求:同 08 号票(做/不做 + 机制边界 + 接缝;借鉴成熟实现——LangGraph time travel、LangSmith replay——并注明来源)。

## Answer

(2026-08-11,grilling 收口,三问全采纳推荐)

**决策:部分做**——L1 只读时间旅行(做,数据基础已齐)+ L2 轻量会话分叉(做);原地 replay、专门断点调试、异常快照机制(不做,已被既有机制吸收)。

**机制边界(管什么/不管什么)**:
- **L1 只读时间旅行(做)**:按轮还原"模型实际所见"——InjectionSnapshot(消息序列+动态预算明细+策略版本,已逐轮落库)+ 消息 + 事件流,数据已齐;dashboard 呈现层按轮查看,不重跑
- **L2 会话分叉(做,轻量)**:从指定轮次复制消息历史到**新 sessionId** 续跑(LangGraph fork 蓝本:parent 指向历史点);不新增 SPI(读 MessageStore 复制 + 续跑);轮次序号/幂等键衔接细则留 Spec
- **重放语义明示**:**不承诺确定性复现**(模型不确定性下"复现"是伪承诺);fork 承诺"同一注入视图 + 同一历史起点的新执行";原轮次模型标识/参数(temperature 等)记入 fork 元数据供对照——漂移如实暴露而非掩盖(InjectionSnapshot 补模型参数快照,小增强)
- **不做原地 replay**(从历史点重跑同会话):LangGraph replay 语义依赖图节点确定性重跑,不周山"消息即检查点"模型下原地重跑语义含糊,且与 05 恢复语义打架
- **不做专门断点调试模式**:断点/改工具返回 = 15 干预平面的调试场景应用(挂起=断点,纠偏注入=改返回),机制已通
- **不做异常快照机制**:注入快照逐轮落库 + 事件流含错误 Event + state 在 SPI——数据已在;08 失控终止/异常时打**标记事件**即可

**接缝**:
- fork 是 **19 评估回归的执行手段**(生产会话 fork 出重放集,换版本/换参对照)
- dashboard = 只读时间旅行呈现层(DashboardQueryService 数据源已具)
- 08:失控终止/异常打标记事件,回放时定位异常点;16:fork 操作本身记治理事件(谁在何会话从第几轮 fork)
- fork 后新会话是独立会话:租约/预算/审计独立起算(fork 源信息进元数据)

**借鉴**:
- LangGraph Time Travel(`get_state_history`/`replay`/`fork` 语义;fork=parent_config 指向历史点、清空 pending writes 强制后续重估——业界最完整时间旅行规范)— https://docs.langchain.com/oss/python/langgraph/use-time-travel
- LangSmith(生产 trace 转数据集回放:回放不只服务调试,也服务版本回归)— https://docs.langchain.com/langsmith/run-backtests-new-agent
- LangGraph Studio(调试 UI 与运行时同源读 checkpoint——dashboard 呈现同构)— https://github.com/langchain-ai/langgraph
- 业界共识判定:时间旅行是 LangGraph 独有一等功能,OpenAI Agents SDK/AutoGen/CrewAI 均空白(tracing 只能看不能重跑,01 票维度 10)——不周山做 L2 分叉属差异化能力,fork 语义以 LangGraph 为蓝本、落点形态 `> 【推演】`(会话模型 vs 图模型差异)
