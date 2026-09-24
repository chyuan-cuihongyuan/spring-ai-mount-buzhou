# impl 2184 — S 会话 S34 Content-Defined Chunking 内容定义分块（spec 5033 / T6167–T6168 / S34）

纵切片：ContentDefinedChunking（core/fs）——Gear 滚动哈希 +
归一化双掩码 + 内容定义边界（圣像钉住 + 局部性钉住）。

- 验证：`mvn -pl buzhou-core test -Dtest='ContentDefinedChunkingTest'` 全绿。
