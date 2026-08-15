# Spec 36 — 导出扩展槽与 dashboard 索引消费

> effort #7（T121–T122 / impl-96–97）。

## §A 导出扩展槽（T121 / impl-96）

- `SessionExport` 增可选 `extensions` 槽（Map&lt;模块名, JSON 段&gt;；8 参构造兼容、
  toJson/fromJson 随档携带）——core 三槽之外的模块数据经 `SessionExportExtension`
  （name/exportSegment/importSegment）进可移植文档。
- `DefaultAgentRuntime.setExportExtensions`（auto-config 注入 `List<SessionExportExtension>`
  bean，装配期一次性）；导出时非空段才入档（失败段 WARN 跳过）；导入按新 sessionId
  回放（失败段 WARN 不回滚三槽——最终一致；未知段 WARN 跳过）。
- **memory `FactsExporter`**（name=`memory.facts`）：facts 建在 state 的 `fact.*`
  命名空间——导出 = `scanByPrefix("fact.")`（spec 33 §C API 复用）StateEntry 原样段；
  导入 = 逐条 put 回（producer/createdTurn/ttlTurns 语义字段无损，与 DefaultFactStore
  读写互通）。

## §B dashboard 消费会话索引（T122 / impl-97）

- `DashboardQueryService.listSessionsFiltered(appId, agentName, status, tagKey, tagValue, cursor, size)`
  → `IndexedSessionPage(items, nextCursor, fromIndex)`：索引装配时走 SessionIndexStore
  （过滤组合 + lastActive 倒序 + 分页探测，DELETED 默认排除）；未装配回退观测留痕
  （无过滤维度、参数被忽略，`fromIndex=false` 让调用方感知降级——诚实口径）。
- `DashboardModule.builder().sessionIndex(store)` 可选注入；前端展示仍 out-of-scope
  （#4 边界——查询服务侧能力就位）。
