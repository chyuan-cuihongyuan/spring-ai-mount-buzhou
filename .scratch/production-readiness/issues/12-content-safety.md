# 内容安全护栏(注入/越狱/输出合规/水印)

Type: grilling
Status: resolved

## Question

> **03 已决(2026-08-11)**:provider 内容拒绝(finishReason=CONTENT_FILTER 静默通道,不抛异常)的**分类与可观测**由韧性层提供(归一化分类含"内容拒绝"类目、afterModel 可见);**治理策略——拦截/替换/告警——归本票**。

框架是否提供**内容安全护栏**?(参考文档七.1:输入违禁词/prompt 注入检测/越狱拦截/输出合规审核;七.4 输出水印溯源)

需回答:
1. **做不做**——注入/越狱检测、输出合规审核是框架职责还是用户业务;检测手段(规则/分类器/外部安全服务 API)框架自研到哪一层、哪层只做插拔
2. **机制边界**——管输入还是输出还是都管;水印/溯源标记做不做(业界成熟度低,可能是"不做"候选)
3. **接缝**——挂 Hook 链哪环(beforeModel 输入检/afterModel 输出检);与长内容护栏、读写护栏的并存关系;**阻断语义**与现有"失败语义非对称"(读侧降级透传/写侧阻断)的一致性;命中记录进 observability

答题要求:同 08 号票(做/不做 + 机制边界 + 接缝;借鉴成熟实现——如 LangChain 生态的 guardrails 集成模式、Llama Guard、业界 prompt injection 研究——并注明来源)。

## Answer

(2026-08-11,grilling 收口,三问全采纳推荐)

**决策:部分做**——框架做"护栏执行平面",不做检测器本身;输出水印不做。

**机制边界(管什么/不管什么)**:
- **护栏执行平面**:输入/输出护栏的挂载点 + 动作语义 + 检测器 SPI(可插规则/分类器/外部服务);内置轻量规则检测器(敏感词/正则,Spring AI SafeGuardAdvisor 同级)
- **不自研注入分类器**:重型注入检测声明外部依赖(Lakera/LLM Guard 等)——对抗性持续运营品,业界集体划界给第三方
- **动作三件套**(可配):阻断 block / 遮蔽 redact / 仅标记放行 flag-only(事件进 observability)——对应"上线初期观测误报→成熟期阻断"的生产演进路径
- **输出不合规带原因重试**(轻量版,借 CrewAI):次数上限,计入 08 步数预算
- **检测器自身故障降级透传 + 事件**(故障≠命中,与安全组件不作可用性单点;与"失败语义非对称"哲学对齐)
- **内容拒绝治理**(03 划入):afterModel 观测到拒绝标记→事件留痕+可配兜底话术(与 03 M2 兜底响应协同)
- **不做**:输出水印(业界无共识实现,对抗性研究阶段)

**接缝**:
- 输入检挂 beforeModel、输出检挂 afterModel(Hook 链);与长内容护栏、读写护栏并存为内置 Hook——**"护栏平面"收敛信号第一块拼图**(12/13/14/16 共性:Hook 链策略执行点+事件留痕+四层配置)
- 命中记录进 observability;策略走 policy 四层配置

**借鉴**:
- OpenAI Agents SDK InputGuardrail/OutputGuardrail + tripwire(并行执行+中断模型)— https://openai.github.io/openai-agents-python/guardrails/
- CrewAI LLMGuardrail(自然语言规则→校验器,带原因重试)— https://docs.crewai.com/concepts/tasks
- Google ADK safety_settings + 回调 guardrail(模型原生+框架规则双层)— https://adk.dev/callbacks/
- LangChain PIIMiddleware(检测器可插/动作可配/作用点可选——13 的主要蓝本)— https://docs.langchain.com/oss/python/releases/langchain-v1
