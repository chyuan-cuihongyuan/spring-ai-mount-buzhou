# 510 — 模块边界守卫测试 + internal 存量清零

**What to build:** ModuleBoundaryGuardTest（源码级双规则守卫，自举归属，import+内联 FQN）+ 8 处存量违规清零（6 类迁出 internal → core.memory/core.token/core.hook）+ 快照再生。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] ModuleBoundaryGuardTest（双规则 + 自举归属 + 内联 FQN 扫描 + 布局 assume）
- [x] 守卫先红后绿（抓到 import 漏掉的内联 FQN 违规）
- [x] 8 处违规清零（core.memory×3 / core.token×2 / core.hook×1 + 测试随迁 + 全部引用点更新）
- [x] API 快照再生（恰 11 行新增零意外）+ api-surface.md 同步
- [x] 七模块测试绿 + 全仓 verify 绿
- [x] spec 707 + README 行

## Done

验证：全仓 `mvn -B -ntp clean verify` 绿。commit 见本轮 `feat(core)` 提交。
