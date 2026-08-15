# impl-96 — 导出扩展槽（SessionExport.extensions + FactsExporter）

**What to build:** 模块自有数据段进可移植导出文档（facts 首个消费者），导入回放最终一致。

**Blocked by:** T114（scanByPrefix）— 已闭合

**Status:** done

- [x] SessionExport 第 9 槽 extensions（8 参构造兼容 + JSON 往返）
- [x] SessionExportExtension 接口 + DefaultAgentRuntime.setExportExtensions（导出非空入档/导入回放 WARN 不回滚）
- [x] auto-config ObjectProvider.orderedStream 注入；memory FactsExporter（fact.* scanByPrefix 无损段）
- [x] 测试：core 扩展往返 + 失败段容忍；memory facts 与 DefaultFactStore 互通——core/memory 全绿；spec 36 §A

## Done

commit：见 git log（impl-96）。
