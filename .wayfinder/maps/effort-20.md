# Wayfinder Map — Buzhou 观测 OLAP JSONL 导出（effort #20）

> effort #20（已闭合 2026-08-29），延续 #5–#19；收口后累计 167 轮 / impl 1–206。
> 本 effort 主线：**观测数据 OLAP 导出**——fog 毕业生：spans/events 只有会话粒度查询面
>（spansOfSession），没有分析型出口；运维想回答「本周哪些模型最慢/哪类工具最常失败」
> 需要把数据搬进 OLAP。借鉴 Langfuse / Helicone（高价值观测平台的 columnar 摄取面：
> 事件一行一记录的 JSONL + DuckDB/ClickHouse read_json 自动装载）——本仓适配为
> core 导出器 + 会话枚举驱动的全量 dump。

## Destination

`ObservabilityJsonlExporter`：spans/events 平铺为一行一 JSON 对象的 JSONL（稳定字段序、
ISO 时间、duration_ms 派生、换行转义保一行一记录）；支持单会话与全量（会话枚举分页
驱动）导出到 Writer；JSON Lines 规范合规（每行独立可解析）——DuckDB/ClickHouse
`read_json_auto` 直接装载；零新配置键、零行为变化（纯新增只读出口）。

## Notes

- 领域/测试哲学/10K★ 政策/AFK 授权：沿用 effort #6–#19 MAP Notes。
- 外部事实源：Langfuse traces ingestion（平铺列 + 批量）；Helicone ClickHouse 列存
  摄取；JSON Lines 规范（每行一个独立 JSON 值，\n 分隔）。
- 本地勘察（2026-08-29）：SpanRecord/EventRecord 为平铺友好 record（attributes/payload
  为 Map<String,Object>——嵌套保留为 JSON 对象列）；ObservabilityStore 有
  spansOfSession/eventsOfSession + listSessionSummaries（分页枚举全量导出的驱动面）。
- 诚实边界：导出是快照非流式（运行中会话导出=当前已落库部分）；attributes 值类型
  由生产方决定（Jackson 序列化失败条目跳过 + 计数，不阻断整体导出）。

## Decisions so far

- **一行一记录 + 平铺标量列**：spanId/parentSpanId/sessionId/turnSeq/kind/name/
  startedAt/endedAt/status/duration_ms + attributes（对象列）；event 同理。
- **换行转义**：JSON 字符串内 \n 由 Jackson 转义（JsonGenerator.writeStartObject 路径
  天然合规）——禁止字符串拼接（注入/坏行风险）。
- **全量导出 = listSessionSummaries 分页驱动**：游标耗尽即全量；单会话直查。
- **序列化失败容错**：单条 attributes/payload 含不可序列化值 → 该条目降级（丢
  attributes 列 + skipped 计数返回），不阻断。

## Not yet specified

- RunawayHook / TokenBudgetHook 计数写路径同型原子化（fog 沿用）。
- OLAP 增量导出（水位游标）与定时任务（需求证据后议）。

## Out of scope

- 沿用 effort #7–#19 Out of scope 全部条目。
- 流式/增量导出；外部 OLAP 连接器；新配置键。

## Tickets

初始 4 张（T269–T272，按轮逐张闭合）：

- [x] [T269 ObservabilityJsonlExporter（单会话/全量 + 平铺 + 容错）](../tickets/T269-jsonl-exporter.md)（impl-206）
- [x] [T270 JSONL 规范红队（行独立解析/转义/空集/坏值容错/字段序稳定）](../tickets/T270-jsonl-redteam.md)（impl-206；7 例含开放 span/分页全量）
- [x] [T271 文档面（runbook 分析节 + api-surface 登记）](../tickets/T271-jsonl-docs.md)（impl-206；快照 +1）
- [x] [T272 里程碑 verify + 收口](../tickets/T272-effort20-closing.md)（全仓 verify 绿；累计 167 轮）
