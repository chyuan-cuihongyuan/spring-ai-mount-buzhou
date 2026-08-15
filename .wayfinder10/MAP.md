# Wayfinder Map — Buzhou 运营可观测闭环与流量治理（effort #10）

> effort #10，延续 #1–#9（#5：22 轮；#6：9 轮；#7：20 轮；#8：20 轮；#9：19 轮，累计 90 轮 / T1–T169 / impl 1–138）。
> 本 effort 主线：**运营可观测闭环与流量治理**——流式指标（TTFT/TPOT/取消分类/慢滴流累计上限）、
> 日志-链路关联（MDC）、turn 反馈捕获与导出衔接、流量治理增量（加权金丝雀/shadow fork/模型池配额）、
> 错误码与退避卫生收口。
> 到达 = 20 轮自迭代落地、全仓 verify 绿、防线（黄金/红队/perf）与文档齐备、MAP 闭合。

## Destination

流式体验可量化（TTFT/TPOT/取消可计）、日志与链路可互查（MDC）、用户反馈可捕获可导出、
模型流量可治理（金丝雀/shadow/池配额）、错误与退避卫生统一；防线与文档同步闭合。

## Notes

- 领域/测试哲学/10K★ 政策/AFK 授权：沿用 effort #6/#9 MAP Notes（Spring AI 2.0.0 单 Agent harness；
  examples 端到端主接缝；语义借鉴零新依赖）。
- 外部研究（2026-08-16，只认 ≥10K★）：LiteLLM（~26K★）Prometheus 面 `time_to_first_token` /
  `latency_per_output_token` / `request_total_latency`——TTFT/TPOT 双源证据；vLLM（~45K★）
  `time_to_first_token_seconds` / `time_per_output_token_seconds` 直方图；两家均无流取消一级指标
  （真空区，可领先）。LiteLLM Router：deployment weight 加权抽取（金丝雀）、per-deployment tpm/rpm、
  cooldown。Langfuse（~31K★）：score 挂 trace 的反馈 API（categorical/numeric + comment，显式/隐式）。
  GenAI OTel semconv 仍 Development 态——**本地已对齐 gen_ai.\* 命名（勘察证实 OtelBridgeSink），不重做**；
  本 effort 不动属性名。gRPC（~42K★）deadline 范式——**本地 TurnDeadline 已实现（勘察证实），不重做**。
- 本地勘察（2026-08-16，只读扫描）：TTFT 零命中（ObservabilityAdvisor adviseStream 无首信号打点）；
  全仓 src/main 零 MDC；反馈面零 API（仅工具纠错通道）；异常基类 BuzhouException+ErrorCode(14 码) 存在
  但 guard(41)/spill(18)/skills(13)/tools(10)/store(16+5) 泛化 throw 未挂码；webhook outbox 与 policy
  轮询退避无 jitter；DefaultAgentSession:82 自认未订阅流计数残留边界；流式 timeout 为相邻信号间隔语义、
  慢滴流无累计上限（DefaultAgentSession:446-450 注释自认）；模型价目已配置驱动（无缺口）。
- 过程教训沿用：下游模块单跑一律 `-am`；断言以端上计数为准；破坏性变更 pre-1.0 允许但入档 api-surface；
  图前勘察可能有误判，实现期纠偏诚实入档。

## Decisions so far

- [T170 TTFT/TPOT 流式指标](tickets/T170-ttft-tpot-metrics.md) — 首内容信号打点（空块不触发）：span 属性 + STREAM_FIRST_TOKEN 事件 + buzhou.model.ttft/tpot timer（预注册；model tag 截断）；非流式零变化；spec 46 §A。
- [T171 流取消分类与慢滴流上限](tickets/T171-stream-cancel-cumcap.md) — buzhou.stream.cancelled{client|deadline|guard} 三路分类 + stream-total-timeout 累计上限（缺省 10m、≤0 关；takeUntilOther 标记异常终结）；spec 46 §B。
- [T172 MDC 会话轮次关联](tickets/T172-mdc-correlation.md) — chat 路径调用线程 MDC 两键（try/finally 必清）；stream 路径实现期裁定不做（信号切线程致清错线程，结构性限制入档）；spec 47 §A。
- [T173 turn 反馈捕获 API](tickets/T173-turn-feedback.md) — rateTurn 三型校验 + state store 持久化（feedback 键前缀可 scan）+ turn.feedback 事件外发；spec 47 §B。
- [T174 反馈导出与评估衔接](tickets/T174-feedback-export.md) — FeedbackExporter（core.feedback 段 + negative 极性 + negativeTurnSeqs 汇总；空段缺席）；往返保真；spec 48 §A。
- [T175 加权金丝雀降级链](tickets/T175-weighted-canary.md) — canary-enabled + weights 会话稳定哈希加权分流（同会话粘住）；目标失败按链序回退含原主模型；默认关零变化；spec 48 §B。
- [T176 shadow fork 探测](tickets/T176-shadow-fork.md) — 主成功后异步裸调用对照（不重放工具循环；并发+日预算护栏；默认关）；shadow.compared 事件；spec 49 §A。
- [T177 模型池 TPM/RPM 配额](tickets/T177-model-pool-quota.md) — 降级/金丝雀候选统一过限流闸+按实际服务模型记账 TPM；remaining gauge；外层锁步与双账并存诚实入档；spec 49 §B。

## Not yet specified

- LLM 响应缓存（语义边界未清，长期 fog；沿用 #8/#9）。
- skill 语义排序 / outbox Redis 服务端 SCAN 下推（沿用 fog）。
- 观测 OLAP/多实例分布式（长期；沿用边界）。
- store 静态加密（运维层职责；沿用 #9 边界）。
- 评估集自动回流（负反馈 turn → 评估数据集）在 T174 后视衔接成本决定是否成票。

## Out of scope

- 沿用 effort #7/#8/#9 Out of scope 全部条目（多实例分布式、FIDES 二期、sub-agent、LLM-as-judge 硬门）。
- GenAI semconv 属性名变更跟随（规范未 stable，冻结现状；仅注记）。
- E2B/Firecracker 真实档落地（沿用 #2 边界）。
- dashboard 前端工程化（沿用 #4 边界）。
- 日志文案全量 i18n 抽取（证据弱，卫生项暂缓；错误码统一先行）。

## Tickets

初始 20 张（T170–T189，均含 AFK 决议，按轮逐张闭合）：

- [T170 TTFT/TPOT 流式指标](tickets/T170-ttft-tpot-metrics.md)
- [T171 流取消分类与慢滴流上限](tickets/T171-stream-cancel-cumcap.md)
- [T172 MDC 会话轮次关联](tickets/T172-mdc-correlation.md)
- [T173 turn 反馈捕获 API](tickets/T173-turn-feedback.md)
- [T174 反馈导出与评估衔接](tickets/T174-feedback-export.md)（blocked-by T173）
- [T175 加权金丝雀降级链](tickets/T175-weighted-canary.md)
- [T176 shadow fork 探测](tickets/T176-shadow-fork.md)
- [T177 模型池 TPM/RPM 配额](tickets/T177-model-pool-quota.md)
- [T178 错误码统一收口](tickets/T178-error-codes.md)
- [T179 退避 jitter 补全](tickets/T179-backoff-jitter.md)
- [T180 未订阅流计数残留](tickets/T180-unsubscribed-stream.md)
- [T181 黄金轨迹 F](tickets/T181-golden-f.md)（blocked-by T173/T175/T176/T177）
- [T182 红队对抗四批](tickets/T182-redteam-surface4.md)（blocked-by T173/T176/T177）
- [T183 perf 哨兵四批](tickets/T183-perf-4.md)（blocked-by T170/T173/T177）
- [T184 examples 演示四批](tickets/T184-demo-4.md)（blocked-by T173/T175/T176）
- [T185 runbook 第六轮](tickets/T185-runbook-6.md)（blocked-by T170/T173/T175/T177）
- [T186 CONTEXT/api-surface 增补](tickets/T186-context-api-10.md)（blocked-by T170–T180）
- [T187 配置元数据四批](tickets/T187-metadata-4.md)（blocked-by T175/T176/T177/T179）
- [T188 里程碑 verify](tickets/T188-milestone-verify.md)（blocked-by T170–T187）
- [T189 收口](tickets/T189-effort10-closing.md)（blocked-by T188）
