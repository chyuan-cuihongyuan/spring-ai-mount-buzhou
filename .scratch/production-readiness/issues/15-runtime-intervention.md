# 运行时干预(暂停/接管/纠偏/kill switch)

Type: grilling
Status: resolved

## Question

> **08 已决(2026-08-11)**:失控检测的强制终止可配置转人工——本票的干预通道是其联动出口。
> **10 已决(2026-08-11)**:长任务工具"挂起-回填"原语(ADK LongRunningFunctionTool 蓝本)归本票同炉——与 HITL 等待本质同构("挂起等外部事件回填"),合并设计。

框架是否提供**运行中会话的干预能力**?(业界对照:LangGraph interrupt/HumanLayer 12-Factor Agents 的 pause/resume;参考文档一.2 会话冻结/解冻)

需回答:
1. **做不做**——暂停/恢复、人工接管、纠偏注入(在线修正 state/prompt)、kill switch,哪些归框架
2. **机制边界**——干预粒度(轮次间暂停 vs 轮次中打断);接管期间模型行为的定义(挂起?人类代答?)
3. **接缝**——与租约(同会话单活跃实例——接管是否=租约转让)的关系;纠偏注入能否复用 Hook→state→Attachment 闭环的 Attachment 通道;与 05 崩溃恢复(检查点)的关系——干预点是否=检查点;干预操作必须留痕进 observability/审计(16)

答题要求:同 08 号票(做/不做 + 机制边界 + 接缝;借鉴成熟实现——LangGraph interrupt、12-Factor Agents——并注明来源)。

## Answer

(2026-08-11,grilling 收口,三问全采纳推荐)

**决策:做**——运行时干预平面(四能力) + 统一挂起-回填原语。

**机制边界(管什么/不管什么)**:
- **干预四能力**(一个平面,共享控制通道与留痕通道):① 暂停/恢复 ② 纠偏注入(人工改写工具入参——借 LangChain edit 决策;推广到 state/prompt 修订——复用 Attachment 通道)③ kill switch(借 AutoGen ExternalTermination:外部注入终止,走执行脊柱取消传播)④ 人工接管(人类代答,以 `author=human-operator` 消息注入会话)
- **两级粒度**:轮次边界挂起(默认,安全)/ 轮次中打断(kill switch、08 失控转人工,走取消传播 + 05 悬空修复语义)
- **接管 ≠ 租约转让**:会话保持单活跃实例,干预经控制通道注入当前实例(租约管执行权,干预是数据面写入+控制信号)
- **统一挂起-回填原语**(10 归入):工具调用(含 HITL 审批、长任务工具)可挂起为 pending 态,**状态可序列化外迁**(等人工/等回调不占线程),外部事件(人工裁决/长任务完成/webhook)回填后续跑;HITL **双模**:阻断式(现状默认,向后兼容)与挂起式(opt-in)
- 所有干预操作留痕进 observability 并喂 16 审计证据链
- **不管**:人工操作台的 UI(归 dashboard/用户系统);干预权限本身的管理(归 14 主体上下文 + 部署侧)

**接缝**:
- 控制通道:注入控制信号与数据面修订(Attachment 通道复用);执行点:执行脊柱(取消传播)+ Hook 链(事件)
- 挂起态存储:复用会话 state/消息 SPI(细则留 Spec);恢复路径与 05 崩溃恢复共享"加载+修复+重驱动"
- 转人工入口:08 失控终止、11 成本硬顶阻断、12 护栏命中均可配置转本平面

**借鉴**:
- LangChain HumanInTheLoopMiddleware(approve/edit/reject/respond 四决策;edit=纠偏注入官方形态)— https://docs.langchain.com/oss/python/langchain/human-in-the-loop
- OpenAI Agents SDK RunState 序列化外迁 + approve/reject 续跑(等人工不占进程)— https://openai.github.io/openai-agents-python/human_in_the_loop/
- Google ADK LongRunningFunctionTool(长任务挂起-回填)— https://github.com/google/adk-python
- AutoGen ExternalTermination(kill switch 官方形态)— https://microsoft.github.io/autogen/stable/reference/python/autogen_agentchat.conditions.html
- LangGraph interrupt()(函数内断点+节点重跑语义)— https://docs.langchain.com/oss/python/langgraph/interrupts
- HumanLayer 12-Factor Agents Factor 6/7(等待人类=未返回的工具调用)— https://github.com/humanlayer/12-factor-agents
