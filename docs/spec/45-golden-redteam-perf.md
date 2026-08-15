# Spec 45 — 黄金/红队/perf 防线（effort #9）

> effort #9 防线 spec。§A：黄金轨迹 E（T161）；§B：红队对抗扩展（T162）；§C：perf 哨兵第三批（T163）。

## §A 黄金轨迹 E（T161 / impl-132）

三条新机制轨迹（沿用黄金集「脚本化输入 → 可观测断言」口径；事件面缺席处以机制可观测 API 断言）：

1. **G19 单飞闸**：首轮工具挂住在途 → 并发入口 TURN_IN_FLIGHT 确定拒绝 → 放行收尾 →
   inFlightTurns 收口归零 → 续轮正常（终点可续）。
2. **G20 审计轮换持久化**：persister 落 v1 → 目录扫描入环 → v1 记录落链 → rotate 写而后切 →
   v2 记录落链 → 「重启」目录扫描重建环全链可验（版本分布 1:1, 2:1）→ 外锚完整通过/删尾检出。
3. **G21 spill 加密往返**：密文落盘（魔法行、无明文）→ load/readRange 透明原文 → sha256 完整性
   锚有效 → 旧明文文件兼容读 → SpillModule 带密钥构造闭环。

### Testing Decisions

- examples golden 包（Prior art：GoldenTrajectoryEffort8Test）；G19 用挂起工具驱动在途，
  G20/G21 直驱机制公开面。

## §B 红队对抗扩展（T162 / impl-133——观察档）

静态安全新攻击面的确定性对抗用例（替身模型域外；promptfoo 词汇不可表达——沿用 effort #8
NewSurfaceAdversarialTest 口径，先观察不进硬门）：

1. **密钥错配**：换钥重启读旧加密文件 → 快速失败（不静默产出脏明文）。
2. **密文篡改**：磁盘位翻转 → GCM tag 验败快速失败。
3. **记录改写**：DB 写权限攻击者改写中间记录 → 链断在首个被改记录（firstBreakIndex 定位）。
4. **删尾/整链重写**：内部一致性自洽场景下，签名与外锚的检测边界诚实钉住——
   纯内部校验的盲区（删尾 intact）由外锚补检；伪造链头 ≠ 外锚即拒。

### Out of Scope

- 转 nightly 硬门（先观察，稳定后按 baseline 定门——沿用 #8 节奏）。
