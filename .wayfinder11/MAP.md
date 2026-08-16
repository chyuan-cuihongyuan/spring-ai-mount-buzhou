# Wayfinder Map — Buzhou 评估闭环（effort #11）

> effort #11，延续 #5–#10（#5：22 轮；#6：9 轮；#7：20 轮；#8：20 轮；#9：19 轮；#10：20 轮，
> 累计 110 轮 / T1–T189 / impl 1–155）。
> 本 effort 主线：**评估闭环（Eval Loop）**——负反馈回流成评估数据集、批次评估 runner、
> 评分器 SPI 与汇总查询、事件外发；把 effort#10 落地的反馈捕获（rateTurn/core.feedback 导出）
> 接成「运营数据 → 评估资产 → 质量回归」的可运营闭环。
> 到达 = 13 轮自迭代落地、全仓 verify 绿、防线（红队/perf）与文档齐备、MAP 闭合。

## Destination

负反馈可一键回流为评估项（带溯源）、评估数据集可治理（命名集合 + 项增删查）、
批次评估可执行可复现（逐项记录 + 汇总 pass-rate）、评估完成可外发（webhook 零改造）；
LLM-as-judge 以 SPI 留口不做硬门（沿用 effort#7 边界）。

## Notes

- 领域/测试哲学/10K★ 政策/AFK 授权：沿用 effort #6/#9/#10 MAP Notes（Spring AI 2.0.0 单 Agent
  harness；examples 端到端主接缝；语义借鉴零新依赖）。
- 外部研究（2026-08-16，只认 ≥10K★）：Langfuse（~31K★）datasets/evals 域模型——
  Dataset（name/schema）→ DatasetItem（input/expectedOutput/sourceTraceId 溯源/版本化）→
  DatasetRun（experiment 批次）→ DatasetRunItem（item↔trace 链接）→ Scores（llm_as_judge /
  code 两类 evaluator）；负反馈经 sourceTraceId 回流溯源；CI 面经 eval queue 异步评分。
  本 effort 语义映射：DatasetItem.sourceTraceId → sourceSessionId+turnSeq；Scores.code →
  Evaluator SPI；DatasetRun → EvalRun（同步顺序执行，不做异步队列——单进程规模不需要）。
  LiteLLM（~26K★）响应缓存研究已完成，留作 effort#12 主题（cache key/后端/per-call 控制面）。
- 本地勘察（2026-08-16，只读扫描）：全仓无通用评估实体（仅 memory 模块 CompactionFidelityEval
  内部压缩保真评估，不通用）；FeedbackExporter.negativeTurnSeqs（impl-143）为回流原料；
  SessionStateStore.scanByPrefix per-session 下推已就绪（impl-136）；WebhookOutbox 合成会话
  先例（SESSION_ID="__buzhou.webhook__"，StoreFsck 对 `__buzhou.*` 天然豁免）；
  AgentSession.chat(String) 为 runner 逐项执行入口；StateEntry 含 producer 字段可挂溯源。
- 过程教训沿用：下游模块单跑一律 `-am`；图前勘察可能有误判，实现期纠偏诚实入档；
  **多构造器嵌套 record 必须显式 @ConstructorBinding（T187 存量缺陷教训，新配置面预防）**。

## Decisions so far

- [T190 评估数据集 store](tickets/T190-eval-dataset-store.md) — 合成会话 `__buzhou.eval__` + 双前缀键 + 治理面五操作；EVAL_OPERATION_INVALID 挂码；deleteDataset 相邻前缀串删纠偏；spec 52 §A。
- [T191 负反馈回流](tickets/T191-feedback-import.md) — isNegative 单一事实源口径复用（提 public）；幂等去重；无回复轮跳过；expected 强制非空（票面「可空」修订）；spec 52 §B。
- [T192 评估器 SPI](tickets/T192-evaluator-spi.md) — SPI + EXACT/CONTAINS/REGEX（JSON_PATH 依赖盘点后不做）；LLM-as-judge 留口；非法正则构造期 fail-fast；spec 52 §C。
- [T193 批次评估 runner](tickets/T193-eval-runner.md) — 项粒度会话隔离 + 三态逐项记录 + passRate 汇总落 `eval.run.<runId>`；异常不断批；勘察纠偏 throwOnCall 时序陷阱；spec 52 §D。
- [T194 评估结果查询](tickets/T194-eval-query.md) — 四查询只读面 + 摘要行 + 倒序 + Optional empty 语义；spec 52 §E。
- [T195 评估事件外发](tickets/T195-eval-events.md) — emitEvent 新公共面 + eval.run.completed（收尾会话口径）+ 空集不发裁定；spec 52 §F。

## Not yet specified

- LLM 响应缓存（LiteLLM 语义研究已备，effort#12 采纳）。
- skill 语义排序 / outbox Redis 服务端 SCAN 下推 / 观测 OLAP / store 静态加密（沿用 fog）。
- 评估异步队列与多实例分布式评分（单进程规模不需要；沿用边界）。

## Out of scope

- 沿用 effort #7/#8/#9/#10 Out of scope 全部条目。
- LLM-as-judge 硬门禁（judge 不确定性与成本；SPI 留口，门禁策略由宿主自定）。
- 评估数据集跨环境导入导出 JSON 化（视衔接成本，或并入会话导出面后续轮）。
- promptfoo/CI 流水线集成（宿主侧职责）。

## Tickets

初始 13 张（T190–T202，均含 AFK 决议，按轮逐张闭合）：

- [T190 评估数据集实体与合成会话 store](tickets/T190-eval-dataset-store.md)
- [T191 负反馈回流 API](tickets/T191-feedback-import.md)（blocked-by T190）
- [T192 评估器 SPI 与内置三评估器](tickets/T192-evaluator-spi.md)
- [T193 批次评估 runner](tickets/T193-eval-runner.md)（blocked-by T190/T192）
- [T194 评估结果汇总查询](tickets/T194-eval-query.md)（blocked-by T193）
- [T195 评估事件外发](tickets/T195-eval-events.md)（blocked-by T193）
- [T196 评估面红队对抗](tickets/T196-eval-redteam.md)（blocked-by T191/T193）
- [T197 评估面 perf 哨兵](tickets/T197-eval-perf.md)（blocked-by T193）
- [T198 评估闭环演示](tickets/T198-eval-demo.md)（blocked-by T193/T194）
- [T199 runbook 第七轮](tickets/T199-runbook-7.md)（blocked-by T193/T194）
- [T200 CONTEXT/api-surface 增补](tickets/T200-context-api-11.md)（blocked-by T190–T195）
- [T201 配置元数据 + 绑定验证](tickets/T201-metadata-5.md)（blocked-by T190–T195；无新键则钉住零键）
- [T202 里程碑 verify + 收口](tickets/T202-effort11-closing.md)（blocked-by T196–T201）
