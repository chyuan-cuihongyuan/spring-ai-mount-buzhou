# impl 2005 — Q 会话 R5 top-p 核采样（spec 3004 / T5009–T5010 / R5）

纵切片：NucleusSampler（core/policy）——累积质量 ≥p 最小核截断 +
核内重归一 + keptCount 读数 + −∞ 禁选 + 参数 fail-fast。

- 验证：`mvn -pl buzhou-core test -Dtest='NucleusSamplerTest'` 全绿。
