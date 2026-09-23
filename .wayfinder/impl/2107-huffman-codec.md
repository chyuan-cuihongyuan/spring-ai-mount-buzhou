# impl 2107 — R 会话 R7 Huffman 前缀码（spec 4006 / T6013–T6014 / R7）

纵切片：HuffmanCodec（core/message）——堆建树码长 + 规范码字推导 +
位打包编解码。

- 验证：`mvn -pl buzhou-core test -Dtest='HuffmanCodecTest'` 全绿。
