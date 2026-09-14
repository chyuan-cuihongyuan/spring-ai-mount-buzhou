# 1094 — 评估门阈值敏感性扫描

**What to build:** GateThresholdSensitivity 纯函数（δ 带计数+翻转分向+敏感率）+ 五测。

**Blocked by:** None.

**Status:** done

- [x] GateThresholdSensitivity（core/eval，private 构造静态面）
- [x] GateThresholdSensitivityTest 五测
- [x] spec 1442 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='GateThresholdSensitivityTest'` 5/5 绿。
