# impl 1569 — 检索结果强度重排器（spec 2018 / T3137–T3138 / R19）

纵切片：`RecallStrengthReranker`（buzhou-memory recall 主）+
`RecallStrengthRerankerTest`（八用例）。相关度×强度加权融合、开闭
装饰、两极退化、稳定排序。

- 测试：`mvn -pl buzhou-memory test -Dtest=RecallStrengthRerankerTest` 8/8 绿。
