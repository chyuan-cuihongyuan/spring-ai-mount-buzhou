# impl 2138 — R 会话 R38 Holt-Winters 季节指数（spec 4037 / T6075–T6076 / R38）

纵切片：HoltWintersIndex（core/metrics）——三参数在线更新 +
两季预热启发式 + forecast + 读数面 + fail-fast。

- 验证：`mvn -pl buzhou-core test -Dtest='HoltWintersIndexTest'` 全绿。
