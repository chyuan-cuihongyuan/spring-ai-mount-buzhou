# impl 2391 — W 会话 W39 XorShift64 伪随机数（spec 9038 / W9077–W9078 / W39）

纵切片：XorShift64 伪随机数——XorShift64（core/metrics）：13,7,17 三移位异或推进 RNG+无偏有界面。

- 验证：`mvn -pl buzhou-core test -Dtest='XorShift64Test'` 全绿。
