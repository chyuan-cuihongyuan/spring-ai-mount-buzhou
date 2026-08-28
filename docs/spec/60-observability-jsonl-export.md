# Spec 60 — 观测 OLAP JSONL 导出（effort #20）

> wayfinder map：`.wayfinder20/MAP.md`（T269–T272）。OSS 借鉴：Langfuse traces 摄取
> 平铺列 + Helicone ClickHouse 列存摄取 + JSON Lines 规范。

## Problem Statement

观测数据（spans/events）只有会话粒度的查询面（spansOfSession/eventsOfSession）与
dashboard 展示面；没有分析型出口。运维想回答跨会话问题——「本周哪个模型最慢」「哪类
工具失败率最高」「turn 深度分布」——只能逐会话手搬数据。缺一个 OLAP 友好的批量导出面。

## Solution

core 新增 `ObservabilityJsonlExporter`：把 spans/events 平铺为一行一 JSON 对象的
JSONL（稳定字段序、ISO-8601 时间、duration_ms 派生列）；单会话直查导出、全量经
会话枚举分页驱动导出到任意 Writer。JSON Lines 规范合规——每行独立可解析，DuckDB /
ClickHouse `read_json_auto` 直接装载。零新配置键、零行为变化（纯新增只读出口）。

## User Stories

1. 作为数据工程师，我希望 spans/events 一键导出为 JSONL，所以能直接装载进 DuckDB/ClickHouse 做跨会话分析。
2. 作为数据工程师，我希望字段是平铺标量列 + attributes 对象列，所以 read_json_auto 能自动推断类型。
3. 作为运维者，我希望 duration_ms 派生列现成可用，所以不用在 OLAP 侧重复解析时间差。
4. 作为运维者，我希望单会话与全量两种范围，所以排障与周报两个场景都覆盖。
5. 作为稳定性要求者，我希望坏值条目被跳过并计数而不是中断整体导出，所以一次坏数据不废一次 dump。
6. 作为红队，我希望每行独立可解析且换行被转义，所以没有多行坏记录破坏装载。
7. 作为红队，我希望字段序稳定，所以同结构 diff/追加装载不漂移。
8. 作为既有用户，我希望这是纯新增只读出口，所以升级零风险。

## Implementation Decisions

- 位置：buzhou-core（session 包，与 FeedbackExporter 同域）；依赖仅 Jackson + SPI 类型。
- span 行字段：spanId/parentSpanId/sessionId/turnSeq/kind/name/startedAt/endedAt/
  status/duration_ms/attributes；event 行字段：eventId/spanId/sessionId/type/
  occurredAt/payload。
- 序列化走 Jackson JsonGenerator（字符串内换行天然转义——禁止手工拼接）。
- 全量导出 = listSessionSummaries 分页耗尽 + 逐会话 spans/events；结果按会话序
  连续写出。
- 不可序列化值容错：该条目丢 attributes/payload 列 + skipped 计数（返回值暴露）。
- 导出结果 record：`JsonlExportResult(sessions, spans, events, skipped)`。

## Testing Decisions

- 好测试钉外部行为：每行独立解析回等值对象、换行/引号负载转义合规、空集空输出、
- 坏值容错计数、全量分页覆盖、字段序稳定（两次导出字节级一致）；不测 Jackson 内部。
- 先例：SessionExport 的 JSON 断言形态沿用。

## Out of Scope

- 流式/增量导出与水位游标；外部 OLAP 连接器；定时任务；新配置键。

## Further Notes

- 导出为快照语义（运行中会话 = 当前已落库部分）——诚实入档。
