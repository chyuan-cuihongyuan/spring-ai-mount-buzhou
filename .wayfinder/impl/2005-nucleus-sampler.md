# impl 2005 — Q 会话 R5 top-p 核采样（spec 3004 / T5009–T5010 / R5）

纵切片：NucleusSampler（core/policy）——累积质量 ≥p 最小核截断 +
核内重归一 + keptCount 读数 + −∞ 禁选 + 参数 fail-fast。

- 验证：`mvn -pl buzhou-core test -Dtest='NucleusSamplerTest'` 全绿。
- **收尾修复**：全 −∞ 守卫 `sum<=0` 对 NaN（−∞−(−∞)）恒 false 漏抛
  ——改 `!(sum>0)` NaN 安全反向判定；**教训入档**：提交门禁不得用
  `mvn | grep` 管道（grep 命中即 exit 0 掩盖 mvn 红退出码）——必须
  显式判 mvn 退出码再链 commit（本轮首例破窗，之后改 rc 门禁）。
