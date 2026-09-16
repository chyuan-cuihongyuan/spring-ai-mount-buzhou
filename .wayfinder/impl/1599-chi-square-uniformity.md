# impl 1599 — 卡方均匀性检验（spec 2048 / T3197–T3198 / R49）

纵切片：`ChiSquareUniformity`（core/eval 主）+ `ChiSquareUniformityTest`
（七用例）。χ² 统计量、内置临界值表、拒绝判定。

- 测试：`mvn -pl buzhou-core test -Dtest=ChiSquareUniformityTest` 7/7 绿。
