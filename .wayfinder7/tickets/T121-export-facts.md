---
Type: task
Status: open
---
## Question

导出面 facts 扩展（T107 fog）：SessionExport 不含 facts（memory 模块内部 DefaultFactStore，不在 BuzhouStores）。扩展口径？

## Resolution

AFK 自决：经贡献者接口而非改 SessionExport 结构。core cleanup 同款思路：`SessionExportContributor`？过重——M1 口径：memory 模块增 `FactsExporter`（MemoryModule.factsExporter()：export(sessionId)→JSON 段 / import(sessionId, json)）；SessionExport 增可选 `Map<String,String> extensions` 槽（模块自定义段：key=模块名，value=JSON 字符串），importSession 回放给注册的贡献者。默认空。产 spec 36 §A + impl-96。
