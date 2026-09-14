# 1165 — guard 孤类装配面

**What to build:** ToolRoleGuardHook/InputFloodGuardHook 的 Builder 装配路径。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] GuardModule.Builder：toolRoleGuard/inputFloodGuard 两族方法 + 字段
- [x] 组装段注册（声明即挂）
- [x] GuardOrphanAssemblyTest 三断言 + guard 334 用例零回归

## Done

验证：`mvn -pl buzhou-guard -am test` 全绿。
