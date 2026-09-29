# impl 2317 — V 会话 V16 SipHash24（spec 8015 / V8031–V8032 / V16）

纵切片：SipHash24（core/crypto）——密钥化 64 位 PRF 哈希（官方向量钉死）。

- 验证：`mvn -pl buzhou-core test -Dtest='SipHash24Test'` 三测全绿。
