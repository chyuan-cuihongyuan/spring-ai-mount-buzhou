# Spec 52 — 评估闭环（effort #11）

> effort #11 第一篇。§A：评估数据集 store（T190）；§B：负反馈回流（T191）；§C：评估器 SPI
> （T192）；§D：批次评估 runner（T193）；§E：结果查询（T194）；§F：评估事件外发（T195）。
> 外部事实源：Langfuse（~31K★）datasets/evals——Dataset/DatasetItem（sourceTraceId 溯源）/
> DatasetRun/Scores（code evaluator）；本篇把该域模型收窄为单进程同步版：合成会话 store +
> 顺序 runner + SPI 评估器。LLM-as-judge 只留口不做硬门（沿用 effort #7 边界）。

## §A 评估数据集 store（T190 / impl-156）

### Problem Statement

负反馈（T173/T174）与黄金轨迹（JUnit 断言）之间缺一层「可运营的评估资产」：没有命名的
评估数据集实体，离线评估无数据可跑、无治理面（增删查）、无溯源。

### Solution

`EvalDatasetStore`（core.eval 包）：SessionStateStore 合成会话 `__buzhou.eval__` 上的
dataset/item 持久化——键前缀 `eval.ds.<name>`（元数据）与 `eval.ds.<name>.item.<id>`
（条目）；scanByPrefix 下推复用（JDBC/Redis 免全量读）。

### User Stories

1. As a 评估工程师, I want 命名数据集与带溯源的评估项, so that 负反馈轮可回流为可治理资产。
2. As a 运维工程师, I want 数据集存 store 跨重启不丢, so that 评估资产与业务数据同等持久。
3. As a 宿主开发者, I want 未使用评估面零影响, so that 既有装配与行为不变。

### Implementation Decisions

- 合成会话 `__buzhou.eval__`（对齐 `__buzhou.webhook__` 先例；`__buzhou.*` 不在 fsck 会话
  全集内天然豁免——口径入档）；StateEntry producer 统一 `eval`。
- dataset 元数据：`{name, description, itemCount, createdAt}`；item：
  `{id, input, expected, sourceSessionId, sourceTurnSeq, createdAt}`（溯源 =
  Langfuse sourceTraceId 收窄为 sessionId+turnSeq；手工项 source 两字段可空）。
- itemId：dataset 元数据内 `nextItemId` 单调递增（单进程顺序写；并发场景 last-writer-wins
  诚实入档）；键序即添加序（零填充 6 位，scan 有序）。
- 校验（fail-fast，BuzhouConfigurationException 风格延续）：name 匹配 `[a-z0-9-]{1,64}`、
  重名 create 拒绝、input/expected 非空、item 只能加进已存在 dataset。
- 新 ErrorCode `EVAL_OPERATION_INVALID`（NON_RETRYABLE）：评估面非法操作统一挂码
  （错误码收口纪律，T178 延续）。
- 删除：deleteDataset 清 ds+item 键（run 记录独立前缀不级联——run 自带 datasetName 快照）。

### Testing Decisions

- core：create/list/addItem/items/删除全链路（含键序断言）；重名/非法 name/空 input/
  未建 dataset 加 item 四拒绝；合成会话写入断言（scanByPrefix 前缀口径）。

## §B 负反馈回流（T191 / impl-157）

### Problem Statement

负反馈数据在 state 里（`buzhou.feedback.` 前缀）但没有自动化通道转成评估项；人工挑轮次
成本高且易漏。

### Solution

`FeedbackImporter.importFromFeedback(sessionId, datasetName)`：scan 会话负反馈（复用
`FeedbackExporter.isNegative` 单一事实源口径）→ 对每个 negative turnSeq 取该轮 user 输入
（input）与 assistant 回复（expected）→ addItem（带溯源）；同 dataset 内同
sourceSessionId+sourceTurnSeq 去重。

### User Stories

1. As a 评估工程师, I want 一键把会话负反馈轮转评估项, so that 坏例自动沉淀为回归资产。
2. As a 评估工程师, I want 回流幂等, so that 重复执行不产生重复项。

### Implementation Decisions

- 返回 `FeedbackImportResult{imported, skippedDuplicate, skippedMissingReply}`——无 assistant
  回复的负反馈轮（如被护栏拦截的轮）跳过并计数（不造空 expected 项）。
- 消息取轮边界：按消息 history 找 turnSeq 对应轮的 user 输入与其后的首条 assistant 文本
  （工具调用消息跳过）；找不到即 skippedMissingReply。
- dataset 必须已存在（fail-fast）；回流不自动建集（防误操作散集）。
- 依赖注入 MessageStore + SessionStateStore + EvalDatasetStore（构造注入，零全局态）。

### Testing Decisions

- core：正负混合反馈回流（只负轮入集、溯源正确）；重复回流幂等（skippedDuplicate）；
  无回复轮跳过；空反馈/无负反馈零导入。

## §C 评估器 SPI（T192 / impl-158）

### Problem Statement

评估结果判定逻辑若硬编码在 runner 内则不可扩展；宿主自定义判定（领域断言）无插入口。

### Solution

`Evaluator` SPI：`EvalScore evaluate(String actual, String expected, EvalItem item)`；
`EvalScore(boolean passed, String detail)`。内置三评估器：`ExactEvaluator`（全等）、
`ContainsEvaluator`（子串）、`RegexEvaluator`（expected 为正则，actual 全文 find）。

### User Stories

1. As a 宿主开发者, I want 实现接口即得自定义判定, so that 领域断言进评估无需改框架。
2. As a 评估工程师, I want 内置三评估器覆盖常见口径, so that 零编码可跑首轮回归。

### Implementation Decisions

- jayway json-path 不在依赖树（勘察证实）→ 第三评估器取 REGEX（JDK 自带；**零新依赖纪律**）；
  JSON_PATH 不做（依赖盘点入档）。
- 非法 expected 正则：构造期 fail-fast（PatternSyntaxException → BuzhouConfigurationException
  带修法）。
- LLM-as-judge：SPI 即口（宿主实现 Evaluator 自行调 judge）；框架不内置 judge、不做门禁
  （边界沿用 effort #7，防不确定性与成本默认化）。
- detail 上限 512 字符（防 actual 全文灌进 run 记录；截断标记）。

### Testing Decisions

- core：三评估器命中/未命中；非法正则构造拒绝；detail 截断；自定义 Evaluator 插入直通。

## §D 批次评估 runner（T193 / impl-159）

### Problem Statement

数据集有了、评估器有了，缺执行面：把 dataset 每项跑一遍 harness 并记录逐项结果与汇总。

### Solution

`EvalRunner.run(datasetName, Evaluator)` → `EvalRunResult`：逐项 spawn 独立评估会话
（appId=`buzhou-eval`、sessionId=`eval-<runId>-i<id>`【回写 2026-08-17：原稿 item<id>，实现取短形】，项粒度隔离、互不污染）→
`chat(item.input)` → 评估器打分 → run 记录 `eval.run.<runId>` 落合成会话；顺序执行。

### User Stories

1. As a 评估工程师, I want 一条命令跑全集并得逐项+汇总结果, so that 回归结论可复现。
2. As a 运维工程师, I want 单项失败不断批, so that 一次 run 得完整画像而非首错即停。

### Implementation Decisions

- runId：`r<epochMillis>-<短随机>`（时间可读 + 撞车防护）；run 记录
  `{runId, datasetName, startedAt, finishedAt, total, passed, failed, errored, passRate,
  items:[{itemId, status(pass|fail|error), detail, actual(截断 2048), durationMs}]}`。
- chat 异常 = 该项 status=error（detail 记异常摘要），继续下一项；run 汇总 passRate =
  passed/total（error 计入 failed 分母口径：passRate = passed / total，failed+errored 同列）。
- 每项会话 try-with-resources close（项结束即释放；不进业务会话计数命名空间——评估面
  隔离口径入档，红队 T196 断言）。
- 空数据集：直接返回零项 run（不失败——空集是合法状态，汇总 0/0；passRate 约定 NaN-safe
  表达为 0 通过 0 总计）。
- 同步顺序执行（单进程规模；异步队列沿用 Out of scope）。

### Testing Decisions

- core：全通过/全失败/单项异常不断批三轨迹；run 记录键与结构断言；项会话隔离断言
  （评估 sessionId 前缀）；空集 run。

## §E 结果查询（T194 / impl-160）

### Problem Statement

run 记录落 store 后没有只读查询面：宿主与运维要自己 scan 解析。

### Solution

`EvalQueryService`：`runs(datasetName)`（列表摘要，startedAt 倒序）、`run(runId)`（完整明细）、
`latestRun(datasetName)`；scanByPrefix 下推复用。

### User Stories

1. As a 评估工程师, I want 按 dataset 查 run 历史与最新结果, so that 质量趋势可看。
2. As a 宿主开发者, I want 只读查询面, so that 报表/UI 不触碰写路径。

### Implementation Decisions

- run 列表项：`{runId, datasetName, startedAt, total, passed, failed, errored, passRate}`
  （不含 items——明细走单 run）；runId 未知返回 Optional.empty。
- 倒序实现：runId 时间前缀天然字典序 → scan 全量后按 startedAt 排序（单 dataset run 数
  有界；不做索引）。

### Testing Decisions

- core：多 dataset 多 run 的列表过滤与倒序；单 run 明细；latest；未知 runId empty。

## §F 评估事件外发（T195 / impl-161）

### Problem Statement

评估完成只有拉模式（查询）；推模式（webhook/监听者）缺位，运营侧无法被动感知。

### Solution

run 完成即发 `eval.run.completed` 事件：payload `{runId, datasetName, total, passed,
failed, errored, passRate, durationMs}`。发送通道 = run 完成后在独立 `eval-<runId>-done` 会话上调
【回写 2026-08-17：原设计为末项评估会话；实现取独立 done 会话——项会话逐项 close 的资源语义优先】
新增公共面 `AgentSession.emitEvent(type, payload)`（default 抛 UOE；DefaultAgentSession
委托 dispatchEvent）——会话已自动挂载全局 listeners（含 WebhookEventForwarder），
webhook 零改造收到。

### User Stories

1. As a 运维工程师, I want 评估完成推送, so that 被动收到质量回归结果。
2. As a 宿主开发者, I want 通用自定义事件出口, so that 领域事件同通道外发。

### Implementation Decisions

- `emitEvent` 进 AgentSession 公共面（rateTurn 同模式 default 方法；api-surface 入档）；
  payload Map 拷贝不可变（SessionEvent compact ctor 已保证）。
- 事件在独立 `eval-<runId>-done` 会话上发【回写 2026-08-17：原稿「末项会话上发（run 会话尚在生命周期内；发完即 close）」，实现以 done 会话专责发事件】；空集 run 无会话——
  走 `SessionEvent.of` 直发全局 listeners？**裁定：空集 run 不发事件**（无评估发生，
  事件语义为「评估完成」非「run 建档」；边界入档）。
- run 失败（如 dataset 不存在 fail-fast）不产生 run 记录也不发事件。

### Testing Decisions

- core：run 完成事件到达自定义 listener（含 payload 断言）；空集不发；emitEvent default
  UOE 语义；forwarder 通道经 listener 注册直通断言。

## Out of Scope（全篇）

- LLM-as-judge 内置实现与门禁（SPI 即口；沿用边界）。
- 评估异步队列/多实例分布式评分（单进程规模；fog）。
- 评估数据集 JSON 导入导出（后续视衔接成本；会话导出面已有扩展机制）。
- promptfoo/CI 集成（宿主侧职责）。
