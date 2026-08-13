# 12 对标开源最优·做完美：core / memory / spill / guard Tier-2/3 落地 Spec

> 本 Spec 由 wayfinder2 图（[MAP](../../.wayfinder2/MAP.md) + [T29–T54](../../.wayfinder2/README.md)）综合而成，事实源 = [docs/research/oss-perfect-tier23.md](../research/oss-perfect-tier23.md)（[T28](../../.wayfinder2/tickets/T28-oss-perfect-tier23-verification.md)，4 并行 research 子 agent 2026-08-14 核验）。遵守仓库铁律 **「改机制先改 Spec」**：落地时须同步修订 [01 记忆压缩](01-memory-compaction.md) / [02 Spill](02-spill.md) / [05 并行工具](05-parallel-tools.md) / [07 Hook 护栏](07-hooks.md) 对应章节，恢复与审计类新增能力落位后可在 `docs/spec/` 增篇或并入最贴近机制篇。领域术语以根目录 `CONTEXT.md` 为准。
> **用户常设授权（2026-08-14）**：全程不需询问意见、按研究推荐迭代（可推翻）。

## Problem Statement

Tier-1（docs/spec/11）已把四机制抬到「对标开源最优」，但横扫 stars ≥ 10K 开源项目后仍有四类用户可感知差距：

- **core**：关键行为（并行工具、错误回喂、Turn 语义、崩溃恢复）**没有确定性回归基建**（Spring AI 9,299★ 不达标且官方无 fake ChatModel）；取消只有「杀线程」一档，无「等当前工具 / 等当前 Turn」语义；恢复是 **reactive**（加载历史时修悬空），进程重启后**无人枚举在途 run 续跑**；工具调用无 exactly-once 证据，崩溃后可能重复执行已完成的写操作；人审（HITL）暂停后 resume 语义缺失。
- **memory**：逐出比例不可配（一次压到底有断崖风险）；对账/去冗余占**热路径**耗时；摘要错误只能等下一次全量压缩自然稀释，agent **无自愈手段**；压缩质量**无持续护栏**（丢关键信息无人知晓、也无回滚）；模糊召回缺失——原文在持久层却只能靠 evidence-id 精确回读，「哪段讲过 X」问不了。
- **spill**：回读只有 byte/jsonpath/pagination，「取头取尾」要模型自己算两次 offset；已消费的旧 tool_result 长期占窗（Anthropic 判定清除它们是「最安全最轻的压缩」）；回读**无法证明切片与溢出时同字节**（腐化/TOCTOU 不可检测）；「哪一段讲了 X」的语义定位缺失；源码按字节切会斩断函数/类。
- **guard**：注入防御是确定性的（spotlighting/canary）但**从未被红队批量验证过**；不可信数据到写侧工具的信息流**无形式化管控**（读写非对称止于失败语义，未覆盖数据流）；授权/审批决策**无防篡改审计链**；`run_command` 只有黑名单+沙箱目录，无进程级硬隔离档位；危险工具规则是 bespoke 配置，**非可分析、非默认拒的 policy**。

## Solution

按研究推荐把 **Tier-2 全量 + Tier-3 精选** 落满四机制，全部以 stars ≥ 10K 开源项目为采纳事实源（注记源仅 informing）：**core** 建 FakeChatModel+record/replay 测试地基、参数 schema 校验+重试预算、CancelMode 三档、Run 注册表+事件溯源工具调用日志+幂等键（proactive 恢复）、interrupt/resume 按 toolCallId、time-travel fork、批提交语义；**memory** evictRatio 部分逐出、sleep-time 后台整理、memory-as-tools 自愈记忆+防投毒（provenance/taint/审计）、压缩保真 eval、压缩前检查点三档回滚、向量 recall 三模搜、episodic few-shot；**spill** head+tail 窗口风味、context-clearing、chunk hash 回读校验、语义回读两段式、AST-aware 切片；**guard** promptfoo 红队门、FIDES 最小 taint 信息流控制、ECDSA 签名审计链、CommandSandbox 三档（Deno 档必做）、policy-as-code 内嵌子集+OPA sidecar SPI、ONNX 分类器（默认关）。既有的真原创**不重做、只强化**。

## User Stories

1. 作为 Buzhou 接入方，我希望**不调真实 LLM 就能确定性回归并行工具/错误回喂/Turn 语义**（FakeChatModel 脚本队列），这样 CI 快、稳、免费。
2. 作为 Buzhou 接入方，我希望**录制一次真实会话即可回放为 fixture**（RecordingChatModel + JSON fixture），这样复杂多轮行为的回归有真实依据。
3. 作为贡献者，我希望**回放失配（请求序列对不上）直接判测试失败**，这样静默漏断言不可能发生。
4. 作为 Buzhou 接入方，我希望**工具参数不过 schema 时不执行、把校验错误回喂模型并扣减每 Turn 重试预算**，这样模型误用工具能自愈、且重试有硬上限。
5. 作为 Buzhou 接入方，我希望**校验重试与执行期错误回喂是两个可区分的词汇**（校验失败 vs 执行失败），这样观测与策略能分别对待。
6. 作为 Agent 终端用户，我希望**取消正在运行的请求时能选「立即 / 等当前工具批 / 等当前 Turn」**，这样取消语义匹配我的意图（立即止损 vs 干净收尾）。
7. 作为 Buzhou 接入方，我希望**取消 token 贯穿嵌套工具链**且长任务可实现可中断接口，这样取消不悬空、不留半成品。
8. 作为 SRE/运维者，我希望**立即取消丢弃在飞工具结果、Turn 后取消保留部分输出入 Completed-Turn**，这样部分更新不会泄漏进历史。
9. 作为 SRE/运维者，我希望**进程重启后能枚举在途 run 并安全续跑**（Run 注册表 + lease 门），这样崩溃从「等用户撞上」变「开机自愈」。
10. 作为 SRE/运维者，我希望**restart 从最后 Completed-Turn 之后续跑且必须先拿到租约**，这样恢复不重跑已完结工作、也不会双实例打架。
11. 作为 Buzhou 接入方，我希望**每次工具调用追加进不可变日志且带幂等键**，这样崩溃恢复时已完成的调用按 id 短路、绝不重复执行写操作。
12. 作为 Buzhou 接入方，我希望**恢复语义是「Completed-Turn 之后续跑」而非全量重放**，这样恢复不会重跑 LLM、成本可控。
13. 作为 Agent 终端用户，我希望**人审挂起后 resume 精确注入对应工具的答复**（按 toolCallId 匹配、可逐个 resume），这样多工具同批挂起也不串线、且 resume 绝不重放 Turn 前段。
14. 作为 Buzhou 接入方，我希望**能枚举历史 Completed-Turn 并从任一检查点 fork 新会话**，这样 what-if 调试与评测有干净的分叉点。
15. 作为 Buzhou 接入方，我希望**并行工具批全部成功才整批入历史**、失败时成功者结果暂存且回喂策略显式可配，这样「事务性」诚实（状态层原子、副作用不谎称回滚）。
16. 作为 Agent 终端用户，我希望**压缩只逐出约 70% 候选并按 10% 步进加压**，这样上下文不断崖、最近原文与既有摘要保持连续。
17. 作为 Agent 终端用户，我希望**对账/去冗余/重排挪到 Turn 后异步整理**（每 session 串行、不阻塞响应），这样长会话响应不因整理变慢。
18. 作为 Agent 终端用户，我希望**模型能自己修正摘要段错误**（精确匹配 + 唯一性检查 + P0 只读锁），这样压缩错误能自愈而非等待全量重压。
19. 作为安全评审者，我希望**记忆写入带 provenance 与 taint 位、untrusted 内容未经脱敏不得进摘要正文**，这样记忆投毒（Unit 42 类攻击）在框架层被挡住。
20. 作为安全审计者，我希望**记忆写操作全量进防篡改审计链**，这样投毒企图事后可查。
21. 作为 Buzhou 接入方，我希望**压缩保真度有持续 eval**（注入 follow-up + evidence-id 断言 + LLM judge + 误触发负例），这样九段式摘要质量有护栏、prompt 变更有回归门。
22. 作为 Buzhou 接入方，我希望**压缩提交前有检查点、可三档回滚**（仅消息窗 / 加撤销摘要生效 / 连事实台账），这样压缩事故可恢复。
23. 作为 Agent 终端用户，我希望**能用 text/embedding/time/hybrid 四种模式模糊召回原文**（含分页游标），这样「之前哪段讲过 X」可回答、无需记住 evidence-id。
24. 作为 Buzhou 接入方，我希望**向量索引与消息台账单库同事务**（pgvector），这样召回与事实源不漂移、不引入独立向量库运维。
25. 作为 Agent 终端用户，我希望**成功任务的经验被采集为 episode 并按预算注入为新任务示例**，这样同类任务越做越好。
26. 作为 Agent 终端用户，我希望**回读大输出可一次取「头+尾」窗口且中段有显式省略标记**（省略量 + offset + 回读指引），这样 schema 在头结论在尾的数据一次看全、且我知道中间少了什么。
27. 作为 Buzhou 接入方，我希望**旧 tool_result 超阈值时自动降级为 Handle 占位（保最近 N 个完整）且由 harness 自持**，这样最安全最轻的压缩对所有模型生效、不依赖厂商 server 侧特性。
28. 作为 Buzhou 接入方，我希望**能显式逐出已消费句柄**（模型主动工具 + 引用计数 TTL 双路径），这样上下文与缓存占用可控。
29. 作为安全评审者，我希望**回读响应附带切片 hash 且校验失败走读写非对称**（读侧 warning / 写侧阻断），这样「回读即原文」有密码学证明、腐化可检测。
30. 作为 Agent 终端用户，我希望**能用语义查询定位溢出数据段落再精读**（semantic locate → byte fetch 两段式），这样大海捞针有勺子。
31. 作为 Agent 终端用户，我希望**源码回读按 AST 边界切片**（Java 全 AST、其他语言分隔符回退、先切再解析、永不静默失败），这样函数/类不被斩断。
32. 作为安全评审者，我希望**guard 配置在 CI 里被红队批量攻击**（promptfoo nightly、先观测不阻塞），这样防御不是「我觉得行」。
33. 作为安全评审者，我希望**不可信数据带 taint 标签、未经审批不得流入写侧工具调用**（FIDES 最小信息流控制），这样间接注入到特权动作的通路被形式化堵死。
34. 作为安全审计者，我希望**HITL 裁决、taint 写门、记忆写入进 ECDSA 签名审计链**（AAT 格式 + JCS + P1363），这样「发生过什么」不可否认。
35. 作为 Buzhou 接入方，我希望**run_command 可升级到进程级硬隔离**（CommandSandbox SPI：Deno 精细授权档必做，Firecracker/E2B 档接口预留），这样爆炸半径按部署需求伸缩。
36. 作为 Buzhou 接入方，我希望**危险工具规则升级为默认拒、可分析的 policy**（内嵌子集 + OPA sidecar SPI），这样授权策略可版本化、可推理、可与业界心智对齐。
37. 作为 Buzhou 接入方，我希望**可加挂概率性注入分类器层**（ONNX Prompt-Guard，optional 依赖、默认关、模型自备），这样纵深多一层但不强迫我接受误报与模型分发负担。
38. 作为 Buzhou 接入方，我希望**以上能力默认安全、按机制开关、不引不达标依赖**，这样升级不破坏既有契约、classpath 干净。
39. 作为贡献者，我希望**改动同步回写机制 Spec 且有既有测试哲学守护**，这样文档与实现不漂移。

## Implementation Decisions

> 不含具体文件路径/代码片段。接口级描述。「采纳」均指研究推荐 shape。Phase 划分供 `/to-tickets` 切片参考，非硬性阶段门。

### 范围与阶段

- **Phase 0 地基**：FakeChatModel + record/replay（多数后续项的回归依赖）。
- **Phase 1 廉价 wins**：evictRatio、head+tail 窗口、参数 schema 校验+重试预算。
- **Phase 2 core 恢复链**：CancelMode → Run 注册表 → 事件溯源工具日志 → interrupt/resume+fork → 批提交。
- **Phase 3 memory 深化**：sleep-time → memory-as-tools+防投毒 → 压缩检查点 → 保真 eval → 向量 recall。
- **Phase 4 spill 深化**：context-clearing → chunk hash 校验 → 语义回读（依赖向量基建）→ AST 切片。
- **Phase 5 guard 形式化**：红队门 → FIDES taint → ECDSA 审计链 → policy 子集 → 分类器 → 沙箱档。
- **Phase 6 长线**：episodic few-shot。

### core

1. **FakeChatModel + record/replay**（T29；源 vercel/ai 26,168★ + pydantic-ai 19,271★，Spring AI 9,299★ 不达标无官方 fake）：`FakeChatModel` 实现 ChatModel/StreamingChatModel，持脚本队列按调用序弹出（耗尽重复末值）；脚本项支持单条 assistant 消息多个 toolCall（并行回放语义）。`RecordingChatModel` 装饰真模型，(request, response) 序列落 JSON fixture；回放按请求序列匹配，**失配即测试失败**。fixture 约定 `recordings/` 资源目录 + 脱敏（真实 key/PII 不落盘）；examples 测试基类提供「防真实请求」全局开关（Pydantic AI `ALLOW_MODEL_REQUESTS` 模式）。
2. **参数 schema 校验 + per-turn 重试预算**（T30；源 pydantic-ai + instructor 13,726★）：工具执行**前**校验 arguments（复用 spring-ai 工具 schema 生成）；失败合成 **`ToolValidationFeedback`**（与执行期 `ToolErrorFeedback` 两档词汇分明，格式对齐 T16 错误回喂）；`TurnLoopPolicy` 增 `retryBudget`（默认 1–2，与 Turn 上界独立扣减），耗尽转 REASK_FAILED 停止条件。
3. **CancelMode 三档 + token 贯穿**（T31；源 autogen 60,404★ + openai-agents-python 28,616★）：`enum CancelMode { IMMEDIATE, AFTER_CURRENT_TOOLS, AFTER_CURRENT_TURN }`；IMMEDIATE=虚拟线程 interrupt、AFTER_CURRENT_TOOLS=等 StructuredTaskScope join；取消 token 贯穿 TurnLoop 与工具执行器，`InterruptibleTool` 可选接口；落盘策略=Turn 后取消保留部分输出入 Completed-Turn、立即取消丢弃在飞结果（防半成品泄漏，吸取 AutoGen partial 丢弃语义）。
4. **Run 注册表 + 枚举续跑**（T32；源 mastra 27,179★）：`RunRegistry`（listRuns 按 status+分页 / getRun / persistRunState，**以 Completed-Turn 为快照单元**）；`RunHandle.restart()`=从最后 Completed-Turn 之后续跑（不重跑已完结 Turn，规避 Mastra restart 重跑缺陷）；**restart 前必须先拿到该 run 租约**（复用既有 SessionLeaseStore SPI），拿不到即拒绝（补 Mastra 未明的并发防护）；InMemory+JDBC 双实现；自动恢复可开关。
5. **事件溯源工具调用日志 + 幂等键**（T33；源 temporal 22,284★ + dapr 26,021★，Restate 注记）：**不引入 workflow engine**。追加式 `ToolCallLog`（turnId、toolCallId、请求指纹 argsHash、outcome）与 RunRegistry 同存储介质；restart 时已落盘 outcome 的 toolCall **按 id 短路不重跑**；幂等键 = `sessionId+turnId+toolCallId` 随调用传给工具端；恢复点=最后 Completed-Turn，**天然不重放 LLM**。
6. **interrupt/resume 按 toolCallId + time-travel fork**（T34；源 langgraph 39,627★ 规避其反模式）：Turn 挂起记录 `pendingToolCalls[]`（toolCallId、args 指纹）；`resume(toolCallId, payload)` 精确注入对应 ToolResponse、可逐个 resume；**绝不重放 Turn 前段**（未落盘 Turn 丢弃重开、同批已执行结果按批记录暂存保留）；与既有 HITL 门合流（挂起=门未放行、resume=授权 payload）。`Session.listCompletedTurns()`（指纹+元数据）+ `forkFrom(turnId)` 复制截至该 Turn 的 history 到新 sessionId 续跑。
7. **事务性并行批——批提交语义**（T35；源 langgraph **修正版**语义：superstep 实为 pending-writes 半事务、兄弟成功写保留、副作用无回滚）：并行工具批=全部成功才整批 ToolResponse 入 history 并落 Completed-Turn；任一失败→失败者走 ToolErrorFeedback、成功者暂存批记录（落 T33 ToolCallLog），**回喂策略显式可配**（全部回喂 / 仅失败回喂）；诚实宣称「状态层原子」，不宣称副作用回滚。

### memory

8. **evictRatio 部分逐出**（T36；源 letta 24,230★：SDK 默认 0.3 摘要/0.7 保留 + 10% 步进）：`evictRatio` 参数化（**默认 0.7**）+ 10% 步进升级梯子（预算仍超时逐级加压）；不变式：**最近 N Turn 原文 + 上一次增量摘要永不逐出**（与 summarized_message_ids 双水位兼容）；P3 段先承受压力（对齐 P0–P3 优先级）。
9. **sleep-time 后台整理**（T37；源 letta）：turn 后 hook 投递 `MemoryConsolidationTask` 到专用 executor（**JDK21 虚拟线程 + 每 session 串行化**防写竞争）；整理动作=SummaryFactReconciler 对账、去冗余、P0–P3 重排、archival 归档，**全走双时序台账**；触发频率可配（每 N Turn）、失败退避重试、可开关、全程审计。
10. **memory-as-tools 自愈记忆 + 防投毒**（T38；源 letta + Unit 42 注记）：暴露 `revise_summary_section(section_id, old_text, new_text)`——**精确匹配 + 多处命中类型化错误 `EDIT_AMBIGUOUS`/`EDIT_NOT_FOUND`** + P0 段只读锁（Letta 防静默覆写机制）；每次写入带 **provenance（来源 message id）+ taint 位**：evidence 源自工具输出的内容标 untrusted，未经脱敏不得进摘要正文、只进 scope 受限 evidence 区（**超越 Unit 42 公开建议水位**）；写操作全量审计日志（衔接 AAT 链）；archival 检索由 T41 三模搜承担，本轮不另暴露工具面。
11. **压缩保真度 eval**（T39；源 langchain 144,172★ trace 注入方法论 + 自建保真断言）：`CompactionFidelityEval`——回放录制会话（依赖 Phase 0 基建）→ 注入仅在压缩前水位之下可答的 follow-up → 压缩后上下文跑 agent → LLM judge + **evidence-id 精确断言**；负例集（不应压缩场景）防误触发；指标=保真率/误触发率/压缩比；评测式断言沿用既有 `SummaryEvaluationTest` 方法论；judge 可 stub 双轨。
12. **压缩前检查点与三档回滚**（T40；源 cline 66,136★）：`CompactionCheckpoint`（sessionId、seq、preWatermark、消息窗引用）在 compact_now/增量摘要**提交前**按水位键不可变快照；回滚三档：①仅恢复消息窗 ②恢复消息窗并撤销摘要生效（双时序 valid_to 表达）③连同事实台账回滚（默认关）；检查点时机=压缩事件（非每次工具调用，成本低且复用 evidence 持久层）。
13. **向量 recall 三模搜**（T41；源 letta `conversation_search`）：`RecallSearchQuery{mode: TEXT|EMBEDDING|TIME|HYBRID, query, start/end, after/before sequenceId, limit}` 落既有消息台账 + pgvector 列 + HNSW；**单库双写、事务内同写**（不引入独立向量库）；过滤工具消息与检索自身消息防递归自指；与 `EvidenceLookupTool` 互补（精确指针 vs 模糊召回）；embedding provider 抽象供 spill 语义回读复用；InMemory 后端降级（TEXT/TIME 可用、EMBEDDING/HYBRID 禁用或降级）。
14. **episodic memory few-shot**（T42；源 langgraph/langchain 文档层模式）：`EpisodeLedger{task_signature, goal, tool_trace_digest, outcome, embedding}`；采集=任务成功判定后 hook（或 sleep-time 蒸馏）；注入=goal 向量召回 top-k 按预算渲染进 system prompt「过往成功示例」块（复用预算渲染机制）；procedural 对接规则库仅注记。

### spill

15. **head+tail 窗口回读风味**（T43；源 codex 105,721★ 反面教材→风味）：`mode=byte` 增 `window=head|tail|head_tail`（headBytes/tailBytes 默认对称）；中段显式标记行 `…[omitted N bytes, offset X..Y; refetch via mode=byte]`（与 T20 显式截断标记统一格式）；原始字节在 spill 存储完整保留、可无损回取（与 Codex 销毁式截断的本质差异）。
16. **context-clearing**（T44；源 Anthropic 判定 + Claude API server 侧方案的 harness 自持版）：ConversationPostProcessor——上下文超阈值时把旧 tool_result **替换为 Handle 占位**（"cleared; refetch via ReadRangeTool(evidence-id)"）保最近 N 个完整；显式逐出=`EvictHandleTool`（模型主动）+ 引用计数 TTL（框架自动）双路径；**跨 provider 由 harness 自持**（对所有模型生效）；cache 意识：在消息窗尾部边界批量清除、避免每 Turn 增量改写；与 hot-tail 分工（hot-tail 管新结果何时溢出、clearing 管旧 tool_result 何时降级为句柄）。
17. **内容寻址 chunk hash 回读校验**（T45；源 git 62,540★ 概念锚点）：落盘时记录 whole-content hash + **每切片 sha256**（可选 Merkle root）；回读响应 envelope 附 `{data, byteRange, chunkSha256, handleRoot}`；校验失败走既有**读侧 lenient（warning）/ 写侧 strict** 非对称；Merkle root 进 evidence-id 的格式演进须兼容旧 handle。
18. **语义回读第 4 模式**（T46；源 letta archival）：durable/cold 层 offload 时**异步**按既有切片边界 embed（**hot-tail 不索引**）；`mode=semantic`（query, k, minScore 可选, tag/filter）返回 top-k chunk 条目（evidence-id + byte offset + 摘要），模型再以 `mode=byte` 精读——**语义是「定位」、既有三模是「取回」，两段式组合**；默认关（依赖 T41 embedding 抽象）。
19. **AST-aware 切片**（T47；达标源 aider 48,169★ + tree-sitter 26,632★ + langchain 144,172★；JVM 绑定全不达标→工程注记）：**Java 全 AST 用 JavaParser**（纯 JVM、非达标源注记）；其他语言=LangChain 式语言分隔符表 + 行边界启发式回退；**先切再解析**（不把整文件/残片喂解析器，避 32KB cliff）、超长行硬截；回退链 AST→分隔符→行边界，永不静默失败。

### guard

20. **CI 自动红队门**（T48；源 promptfoo 24,206★，红队唯一达标源）：examples 暴露测试用 HTTP target（OpenAI 兼容端点包 agent loop）；仓库 `redteam/` 配置（注入类 plugins 优先，对准 spotlighting/canary/HITL 门）；**nightly 流水线、先观测不阻塞 PR**；攻击/评分模型 CI 用 stub/离线、本地可接真模型。
21. **FIDES 最小 taint 信息流控制**（T49；源 MSRC FIDES 论文注记，AgentDojo 注入归零、效用损失 4.5–16.2%）：**最小可行=只标 + 写门校验**——读侧 hook 给工具/RAG 输出 Attachment 打 `TaintLabel`（枚举 TRUSTED/UNTRUSTED + 来源，与 T38 provenance 同源）；LLM 响应保守 join 传播进会话状态；写门（物理阻断 + HITL 工具调用前）校验「上下文标签 ⊔ 实参标签」，失败走既有 session-state 授权 + TTL（= FIDES approver 等价物）；二期（变量隐藏/隔离 LLM/类型容量格）留 fog。
22. **ECDSA 签名审计链**（T50；源 IETF AAT 草案注记）：审计记录 11 字段 + `prev_hash=SHA-256(JCS(prev))`（**RFC 8785 JCS 强制**，JDK 无内置→自写约 200 行、零新依赖）；签名可选 ECDSA P-256，输出转 **IEEE P1363 r||s 64 字节**（JDK DER 转换约 30 行）；session 收尾 session_hash；覆盖=HITL 裁决、taint 写门、记忆写操作。
23. **CommandSandbox 三档**（T51；源 deno 108,248★ / firecracker 36,040★ / E2B 13,383★ 全达标）：`CommandSandbox` SPI；**Deno 档必做**（跨平台、deny-by-default、`--allow-read/net/env(变量名白名单)/run` 精细授权、secret 经白名单透传）；Firecracker 档（root+Linux-only、jailer 全参）与 E2B 档（REST）**接口预留、实现按部署需求**；全部 optional 探测 + Linux/KVM gating；既有 FileSandbox/黑名单降为「无沙箱依赖内联档」。
24. **policy-as-code 内嵌子集 + OPA sidecar SPI**（T52；源 opa 12,099★ 概念、cedar 系不达标注记）：内嵌**自有可分析子集**——声明式 JSON 规则（主体 × 工具 × 资源 × label 谓词 → allow/deny/escalate），默认拒、决策附 reason，语义对齐 OPA「结构化 input→决策」；`PolicyEngine` SPI + **OPA sidecar adapter**（opa-java optional）给 Rego 全表达力部署方；label 谓词衔接 taint 标签（T49）；cedar-java 注记备选；既有危险工具门配置迁移为子集的一个自然特例。
25. **分层分类器**（T53；承载源 onnxruntime 21,369★ 达标、模型 HF gated 注记）：`InjectionClassifier` 接口 + 默认 `OnnxPromptGuard`（onnxruntime Java **optional** + 模型探测式加载、**用户自备下载**）；8B 后置分类器=外部推理服务委托（接口预留）；**默认关**（确定性层已备，分类器是概率纵深）；模型分发细节（路径/校验和/版本钉住）实现时定。

### 横切

26. **依赖卫生**：本轮**不引入任何不达标依赖**进 classpath（cedar-java/NeMo/Rebuff/PyRIT/garak 均注记或出界）；JavaParser 为 spill 的**首个非 Spring 系重依赖**，须评估 optional/坐标；onnxruntime/opa-java 均为 optional 探测。
27. **观测**：新增机制点全部发既有 Event/Span 语义（turn 取消、run 恢复、整理完成、taint 拦截、policy 裁决、审计链校验失败等），保持认知可观测口径。

## Testing Decisions

- **好测试只测外部行为，不测实现细节**；优先复用既有接缝、尽可能用最高接缝，理想只有一个端到端接缝。
- **主接缝（一个、最高）：examples 端到端 agent session**——本轮起以 **FakeChatModel/record-replay（Phase 0 产物）** 为主要驱动（既有脚本化/反应式 ChatModel 测试自然迁移或并存）。断言外部行为：
  - 参数校验失败→模型收到校验反馈、预算耗尽优雅终止；
  - 三档取消→部分结果保留/丢弃语义正确；
  - 重启后枚举续跑→从最后 Completed-Turn 续、不重跑已完成工具（幂等短路可观测）；
  - interrupt/resume→按 toolCallId 精确注入、多挂起逐个 resume；
  - fork→新 session 历史与检查点一致；
  - evictRatio→最近原文与摘要连续（保真断言）;
  - 三模搜→text/time 必答、embedding/hybrid 在 pgvector 后端答；
  - head+tail→省略标记显式、refetch 无损；
  - context-clearing→旧 tool_result 变 Handle、最近 N 完整、回读仍原文；
  - chunk hash→篡改落盘数据后读侧 warning/写侧阻断；
  - 语义回读→locate 返回 offset 后 byte 精读闭环；
  - AST 切片→函数边界完整、未知语言回退不静默；
  - taint→untrusted 上下文中的写侧调用被写门拦截/转 HITL；
  - policy→默认拒、escalate 走 HITL、reason 可观测；
  - 审计链→篡改任一记录后校验失败。
- **次接缝（端到端不实用处）**：既有模块单测扩展（工具执行管理器、Turn 循环、压缩器、摘要对账、溢出钩子、Hook 链）+ 新 InMemory/JDBC 双实现的 store 契约测试（沿用 `AbstractBuzhouStoresContractTest` 范式）。
- **评测式断言**：保真 eval 沿用 `SummaryEvaluationTest` 方法论；eval 是「护栏制品」也是可运行测试（gated judge 可 stub）。
- **红队**：promptfoo 作为独立 nightly 工作流（非 `mvn verify` 的一部分），先观测不阻塞。
- **真实 LLM**：既有 gated 集成测试（`BUZHOU_LLM_API_KEY`）不动；录制 fixture 即来自其手动运行产物。
- **先验**：spec 11 落地时的既有测试即本轮范式（错误回喂/有界 Turn/两级保留/预算渲染等断言风格）。

## Out of Scope

- **MCP widget/structuredContent 双受众 UI 渲染 + poll_token 下载端点**（headless 库无 UI 面；Handle 可借鉴 outputSchema 形状作注记）。
- **Rebuff 依赖**（已归档；canary 已自研）；**NeMo Guardrails / guardrails-ai / PyRIT / garak 依赖**（均 <10K★；概念已注记萃取；红队门用 promptfoo）。
- **Temporal/Dapr workflow engine 整体引入**（只取事件溯源+幂等键思想；Completed-Turn 恢复替代全量 replay）。
- **FIDES 二期**（变量隐藏 Hide/Expand、隔离 LLM+约束解码、类型容量格）——MVP 落地后按效用损失实测再启。
- **Firecracker / E2B 沙箱档的完整实现**（本轮只交付 SPI + Deno 档 + 两重载档的接口预留）。
- **跨 agent/线程共享记忆块、sub-agent/multi-agent 编排架构**（前提能力不存在，另行 effort）。
- **非 core/memory/spill/guard 模块做深**、发布 Maven Central、examples 超出既有 demo+集成测试的扩展（沿用 effort #1 边界）。
- **多实例分布式接管**（lease 升级分布式锁+心跳、Run 注册表跨实例）——单实例语义先行，需求浮现再启。

## Further Notes

- **事实来源**：`docs/research/oss-perfect-tier23.md`（T28，star 数为 2026-08-14 GitHub API 精确值）；相对第一轮研究的 8 项关键修正已吸收（superstep 语义、Codex 头尾各半、poll_token 非标准、OPA 无 JVM 内嵌、org 迁移、Letta 唯一性检查、Spring AI 不达标、graphiti/E2B/promptfoo 达标）。
- **决策票据**：wayfinder2 [T29–T54](../../.wayfinder2/MAP.md) 随本 Spec 批准而闭合（用户常设授权 ratify、可推翻）；执行切片 = `/to-tickets` → `.wayfinder2/impl/`。
- **Spec 同步义务**：落地时同步修订 `01-memory-compaction.md`（evictRatio/整理器/自愈工具/检查点/保真 eval/三模搜/episodic）、`02-spill.md`（窗口风味/clearing/hash 校验/语义回读/AST 切片）、`05-parallel-tools.md`(校验重试/CancelMode/批提交/interrupt/fork)、`07-hooks.md`（taint/policy/审计链/分类器/沙箱）；恢复链（Run 注册表/事件溯源）可增篇或并入 05。
- **测试接缝确认**：沿用 spec 11 判定（examples 端到端主接缝 + 既有模块单测次接缝），新增 FakeChatModel 为基建——用户常设授权下免问询采纳（可推翻）。
- **语言与许可**：文档与注释主语言中文；坐标 `io.github.chyuan-cuihongyuan:buzhou-*`，Apache-2.0。
- **反模式（勿踩）**：checkpoint-only 宣称 durable；resume 重放 Turn 前段；销毁式截断与无标记静默截断；一次逐出 100%；让模型自决审批；单一审核模型当唯一防御；副作用谎称回滚；全量重摘要；裸路径占位符；读写失败无差别；把 access token 当完整性证明。

## 落地记录（2026-08-14）

**27/27 实现纵切片全部落地**（wayfinder2 impl-01..27；`.wayfinder2/impl/README.md` 索引含逐片状态）。本地 `mvn -B -ntp clean verify` **16 模块 BUILD SUCCESS**（576 tests / 0 failures / 0 errors / 30 skipped——skip 为 MySQL/PG/Redis 门控与既有 gated 用例）。

- **core**：FakeChatModel+record/replay 测试基建（01）；参数 schema 校验+retryBudget/REASK_FAILED（04）；CancelMode 三档+取消令牌（05）；Run 注册表+lease 门（06）+事件溯源 ToolCallLog exactly-once 回放（07）；interrupt/resume 按 toolCallId 注入式恢复（08）；SessionForks 检查点分叉（09）；BatchFeedbackPolicy 批提交语义（10）。
- **memory**：sleep-time 后台整理（11）；revise_summary_section 自愈+防投毒（12）；压缩前检查点三档回滚（13，按 Turn 对齐修复多次 get 撕裂）；保真 eval 确定性 judge（14）；recall_search 四模模糊召回（15，消息台账单源+provider 降级）；EpisodeLedger episodic few-shot（26）。
- **spill**：head+tail 窗口风味（03）；context-clearing 句柄生命周期双路径（16）；内容寻址回读校验（17）；语义定位 locate→fetch（18）；语言感知切片 Java AST-lite（19）。
- **guard**：promptfoo 红队门 nightly（20）；FIDES 最小 taint 写门（21）；AAT 审计链+JCS 自实现+ECDSA P1363（22）；policy-as-code 内嵌子集（23）；ONNX 分类器编排层（24）；CommandSandbox 三档（25，Deno 档零内层 shell）。
- **交互缺陷修复（实现中发现）**：OnFailReaskIntegrationTest 预期对齐 impl-04 新契约（schema 拦截先于执行、REASK 预算先于 Turn 上界）；回滚标记按 Turn 对齐（一次 chat 多次 get 的一致性）；T19×T30 词汇分化（校验失败 vs 执行失败）。
- **机制 Spec 同步**：01（evictRatio/sleep-time/revise/checkpoint 配置）、02（窗口风味/clearing/完整性/语义定位/切片）、05（测试基建/校验重试/取消/proactive 恢复/fork/批提交）、07（taint/审计链/policy/分类器/沙箱 + 纵深序更新）。
- **依赖卫生**：全程零新增不达标依赖（JavaParser 6.1K★ 以零依赖 AST-lite 替代；onnxruntime/opa-java 为部署侧 optional；cedar/Rebuff/NeMo/PyRIT/garak 均未引入）。
