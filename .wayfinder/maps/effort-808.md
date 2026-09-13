# effort #808 — 导出内容去重统计

- 会话：H 会话 800 系第 9 轮 ｜ spec [808](../../../docs/spec/808-export-dedupe-stats.md) ｜ 票 [T1117](../tickets/T1117-export-dedupe-stats.md)/[T1118](../tickets/T1118-export-dedupe-stats-verify.md) ｜ impl561
- 借鉴：restic（restic/restic ≈31K star）dedupe stats——逻辑大小 vs 打包大小节省口径

## 勘察（排重）

- SessionExportSizeAudit（738）：体积去向归因（分段占比）——无重复维度。
- SessionExportChecksum：完整性校验非重复统计。
- grep -i `dedup`：EventDeduplicator 是事件分发域——导出域缺位。

## 决定

`ExportDedupeStats`（core.export，纯函数）：analyze(blocks)——精确串值去重键（相似度归其他族，口径声明）；total/unique 条数与字符数、duplicateChars=Σ(count−1)×len、savingsRatio=duplicate/total；Top 重复块按浪费字符降序封顶 16+preview 截 32 字符；空块（null/空白）计 totalItems 不计重复（输入即事实）。字符口径与 738 一致（非字节精确值）。

## 测试

重复计数+节省 10 字符精确+占比 1e-9/全唯一零节省/空块计 items 不计重复/Top 排序 80>12>3+封顶/预览 33 字符截断/空列表+fail-fast——6 例全绿。

## 诚实边界

精确重复口径（相似块归近重复族——803 RRF 相邻域）；字符长度非 UTF-8 字节（口径一致性取舍）；不做实际去重打包（restic 是备份器——本类只做统计面）。
