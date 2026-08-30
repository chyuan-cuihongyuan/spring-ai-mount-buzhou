# 03 — spill · head+tail 窗口回读风味 + 显式中段标记

**What to build:** 模型一次回读即可取「头+尾」窗口（schema 在头、结论在尾），中段以显式省略标记行替代（省略量 + offset + 回读指引），原始字节在 spill 存储完整保留、可无损回取。

**Blocked by:** None — can start immediately.

**Status:** done（2026-08-14：RangeReadRequest.Window + RangeReadEngine.readWindow + read_range schema/解析；RangeReadWindowTest 6 例含无损回取闭环；spec 02 增「head+tail 窗口回读风味」节）

- [ ] `mode=byte` 增 `window=head|tail|head_tail` 风味参数（headBytes/tailBytes 默认对称）
- [ ] 中段显式标记行：`…[omitted N bytes, offset X..Y; refetch via mode=byte]`（与 T20 显式截断标记统一格式）
- [ ] 标记后按 offset refetch 返回无损原文（端到端断言）
- [ ] 既有回读测试全绿 + 新增窗口风味用例
- [ ] spec 02（Spill）同步

> spec 12 §spill-15；[T43](../tickets/T43-spill-head-tail-window.md)。源：codex 105,721★ 反面教材（头尾各半掐中间、无省略标记）→ 取其风味、去其销毁。
