# 1062 — 多租户配额公平指数

**What to build:** FairnessIndex 纯函数（Jain 指数+dominantShare+份额降序+哨兵）+ 五测。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] FairnessIndex（core/budget，private 构造静态面）
- [x] FairnessIndexTest（均匀/倾斜精确值/哨兵/排序/重载）
- [x] spec 1409 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='FairnessIndexTest'` 5/5 绿。
