# impl 2139 — R 会话 R39 Theil-Sen 稳健斜率（spec 4038 / T6077–T6078 / R39）

纵切片：TheilSenSlope（core/metrics）——成对斜率中位数 +
Sen 截距 + 下中位确定性 + 退化 fail-fast。

- 验证：`mvn -pl buzhou-core test -Dtest='TheilSenSlopeTest'` 全绿。
