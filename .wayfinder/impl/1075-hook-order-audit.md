# 1075 — Hook 顺序碰撞审计

**What to build:** HookOrderAudit 纯函数（analyze→Report 同序碰撞组 order 升序/组内字典序）+ 五测。

**Blocked by:** None.

**Status:** done

- [x] HookOrderAudit（core/hook，private 构造静态面）
- [x] HookOrderAuditTest 五测
- [x] spec 1422 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='HookOrderAuditTest'` 5/5 绿。
