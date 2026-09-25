# impl 2221 — T 会话 T21 Geohash 地理哈希（spec 6020 / T6241–T6242 / T21）

纵切片：Geohash（core/policy）——经纬交替二分 Base32 编码
（源码随 T18 对账批预入档；ezs42 经典锚钉住）。

- 验证：`mvn -pl buzhou-core test -Dtest='GeohashTest'` 全绿；随 T18 verify 三门绿。
