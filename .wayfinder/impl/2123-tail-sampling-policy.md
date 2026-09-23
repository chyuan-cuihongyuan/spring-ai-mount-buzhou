# impl 2123 — R 会话 R23 尾采样策略（spec 4022 / T6045–T6046 / R23）

纵切片：TailSamplingPolicy（core/observability）——金料通道 +
概率基线 + 预算封顶 + 理由面/守恒账。

- 验证：`mvn -pl buzhou-core test -Dtest='TailSamplingPolicyTest'` 全绿。
