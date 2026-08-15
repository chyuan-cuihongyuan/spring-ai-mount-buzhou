# impl-109 — 跨 store 迁移器

**What to build:** 会话级跨 store 搬迁（export→import 管线复用）。

**Blocked by:** None

**Status:** done

- [x] SessionMigrator.migrate（静态；重映射/keepIds 沿用；指标 buzhou.session.migrations）
- [x] core 同构用例（三槽全量/新 Id 续用/keepIds fail-fast）2/2 绿
- [x] 依赖方向注记（跨 store 用例归 examples——T145 演示承接）；spec 38 §B

## Done

commit：见 git log（impl-109）。
