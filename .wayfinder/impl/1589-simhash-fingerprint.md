# impl 1589 — SimHash 近重复指纹（spec 2038 / T3179–T3180 / R39）

纵切片：`SimHashFingerprint`（core/metrics 主）+
`SimHashFingerprintTest`（八用例）。加权投票指纹、汉明距离、阈值
判定、平局噪声边界。

- 测试：`mvn -pl buzhou-core test -Dtest=SimHashFingerprintTest` 8/8 绿。
- 教训入档：短文本平局 bit 噪声——测试要钉确定性数学性质（重复词元
  距离 0）而非构造场景绝对阈值。
