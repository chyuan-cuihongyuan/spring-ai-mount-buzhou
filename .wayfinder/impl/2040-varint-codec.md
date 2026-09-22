# impl 2039 — Q 会话 R40 varint 编解码（spec 3039 / T5079–T5080 / R40）

纵切片：VarintCodec（core/message）——zigzag + LEB128 + 游标解码
+ 长度阶梯。

- 验证：`mvn -pl buzhou-core test -Dtest='VarintCodecTest'` 全绿。
