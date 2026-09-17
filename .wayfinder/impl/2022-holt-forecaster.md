# impl 2022 — Q 会话 R22 Holt 预测器（spec 3021 / T5043–T5044 / R22）

纵切片：HoltForecaster（core/metrics）——水平+趋势双分量递推 +
步进外推 + 空态诚实。

- 验证：`mvn -pl buzhou-core test -Dtest='HoltForecasterTest'` 全绿。
