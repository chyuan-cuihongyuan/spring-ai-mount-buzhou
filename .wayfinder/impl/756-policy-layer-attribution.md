# 756 — 策略层级归属读面

**What to build:** PolicyLayerAttribution record（dottedKey/Layer/value + ABSENT 校验）+ LayeredPolicy.getAttributed 归因解析 + get() 薄封装重构 + 归属与等价性测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] PolicyLayerAttribution（嵌套 Layer 枚举 + 构造校验）
- [x] LayeredPolicy.getAttributed（同序同判）
- [x] get() 薄封装重构（逐位零行为变化）
- [x] LayeredPolicyAttributionTest（三层胜出/ABSENT/嵌套路径/中途标量/get 恒等式/构造校验）
- [x] spec 1003 + README 行 + API 快照与 api-surface.md 增行

## Done

验证：`mvn -pl buzhou-core test -Dtest='LayeredPolicyAttributionTest,LayeredPolicyTest'` 全绿。commit 见本轮 `feat(core)` 提交。
