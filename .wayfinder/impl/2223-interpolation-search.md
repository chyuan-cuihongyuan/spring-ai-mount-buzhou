# impl 2223 — T 会话 T23 Interpolation Search 插值查找（spec 6022 / T6245–T6246 / T23）

纵切片：InterpolationSearch（core/concurrent）——double
先行内插+钳制正确性（源码随 T18 对账批预入档；极值差值
long 溢出由 MIN/0/MAX 用例钉住改 double 先行）。

- 验证：`mvn -pl buzhou-core test -Dtest='InterpolationSearchTest'` 全绿；随 T18 verify 三门绿。
