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
