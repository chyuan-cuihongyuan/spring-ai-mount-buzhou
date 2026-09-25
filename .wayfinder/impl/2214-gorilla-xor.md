# impl 2214 — T 会话 T14 Gorilla XOR 浮点压缩（spec 6014 / T6227–T6228 / T14）

纵切片：GorillaXor（core/message）——三态控制位流 + rawBits
按位无损 + 压缩率对账（初稿骨架半成品重写为完整实现；
doubleToLongBits 规范化 NaN 载荷改 rawBits）。

- 验证：`mvn -pl buzhou-core test -Dtest='GorillaXorTest'` 全绿（MVN_EXIT=0）。
