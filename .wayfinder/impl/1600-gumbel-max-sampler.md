# impl 1600 — Gumbel-max 采样器（spec 2049 / T3199–T3200 / R50）

纵切片：`GumbelMaxSampler`（core/policy 主）+ `GumbelMaxSamplerTest`
（六用例）。免归一化采样、禁选 -∞、频率对账。

- 测试：`mvn -pl buzhou-core test -Dtest=GumbelMaxSamplerTest` 6/6 绿。
