# impl 2010 — Q 会话 R10 ARC 自适应替换缓存（spec 3009 / T5019–T5020 / R10）

纵切片：AdaptiveReplacementCache（core/cache）——四链 + p 自适应 +
REPLACE + 命中晋升 + 四读数对账面。

- 验证：`mvn -pl buzhou-core test -Dtest='AdaptiveReplacementCacheTest'` 全绿。
