# impl 561 — ExportDedupeStats（effort #808）

## 切片

- `buzhou-core/src/main/java/.../core/export/ExportDedupeStats.java` — HashMap 计数+uniqueChars 求和+Top 排序封顶 16+preview(32)。
- `buzhou-core/src/test/java/.../core/export/ExportDedupeStatsTest.java` — 6 例。

## 口径

- duplicateChars = totalChars − uniqueChars（守恒校验：测试锁定 total=unique+duplicate）。
- TOP_LIMIT=16、preview 截 32+「…」（隐私：导出统计不复制全文）。

## 验证

mvn -pl buzhou-core -am test -Dtest='ExportDedupeStatsTest' → 6/6 绿；快照再生 1 新公共类型。
