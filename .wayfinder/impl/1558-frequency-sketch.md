# impl 1558 — 频率素描（spec 2007 / T3115–T3116 / R8）

纵切片：`FrequencySketch`（core/metrics 主）+ `FrequencySketchTest`
（六用例）。4bit Count-Min 板、相邻槽较小者 increment、min 读数下界
语义、饱和 15、确定性散列。

- 测试：`mvn -pl buzhou-core test -Dtest=FrequencySketchTest` 6/6 绿。
- 教训入档：门控语义的测试期望要先钉「序」再钉「值」——min 槽读数
  是有偏半值，精确断言必然误红。
