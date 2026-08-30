# impl-83 — Store fsck 一致性校验

**What to build:** 一条只读命令对账五 store（孤儿摘要/残留 state/泄漏租约/悬挂观测），
人读报告 + 按检测项可选修复（观测永不自动清）。

**Blocked by:** T105（引用账本口径确认）——已闭合

**Status:** done

- [x] `StoreFsck.run/repair`（静态入口，RepairOptions 默认全 false）+ 会话全集分页（数字游标）
- [x] `StoreIntegrityReport`（findings/countsByCheck/samples≤20/renderText）
- [x] 测试：干净库/四异常各自命中/extras 扩全集/修复选择性清除——core 281/281 绿
- [x] spec 29 新篇 + runbook 排查树引用

## Done

commit：见 git log（impl-83）。
