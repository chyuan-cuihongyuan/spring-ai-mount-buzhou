# impl 2120 — R 会话 R20 QUIC 反放大窗（spec 4019 / T6039–T6040 / R20）

纵切片：QuicAmplificationWindow（core/backpressure）——×3 信用窗 +
初始授信 + 验证解除。

- 验证：`mvn -pl buzhou-core test -Dtest='QuicAmplificationWindowTest'` 全绿。
