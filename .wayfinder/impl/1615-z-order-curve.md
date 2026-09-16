# impl 1615 — Z 序曲线（spec 2064 / T3229–T3230 / R65）

纵切片：`ZOrderCurve`（core/recovery 主）+ `ZOrderCurveTest`（七用例）。
位交织编码、互逆解码、格内紧致。

- 测试：`mvn -pl buzhou-core test -Dtest=ZOrderCurveTest` 7/7 绿。
- 教训入档：Z 序局部性的断言要求数学可证的口径（格内紧致），逐对
  邻域紧界是假命题。
