# impl 2384 — W 会话 W32 Karatsuba 大数乘法（spec 9031 / W9063–W9064 / W32）

纵切片：Karatsuba 大数乘法——KaratsubaMultiplication（core/crypto）：对半分裂三次子乘递归大数乘。

- 验证：`mvn -pl buzhou-core test -Dtest='KaratsubaMultiplicationTest'` 全绿。
