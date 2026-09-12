# Wayfinder Map — Buzhou 装配绑定审计修复（effort #531，E 会话第 32 轮）

> E 会话第 32 轮（R31 装配审计发现的系统性坑修复轮）。勘察：单 Map
> 组件 record 构造绑定在 prefix.<组件名> 子路径——凡 yml 键声明在
> prefix 根的装配（409 result-schemas/406 deprecated/505 experiments）
> 全部绑空静默 no-op（hasBean 断言挡不住）。实证：condition 根绑定
> 看到值、properties bean 为空。

## Destination

四处装配统一改根绑定直读（bean 内 Binder.get(env).bind(prefix,
mapOf)——409 String/406 Spec record/505 嵌套 Object→Number|String 权重
转换/502 capability 同修于 R31 排查）：ToolResultSchemaHook +
schemasCount() 读数；各处回归断言从 hasBean 升级为内容非空。

## Notes

- 号段：spec 531 / T815–816 / impl-434。
- 根因：Spring Boot 构造绑定 Map 的组件名即命名空间（非根）。

## Out of scope

- 其余嵌套 record 型（组件名在 yml 路径中——422/426/524 正确绑定）。

## Tickets

- [x] [T815 四处根绑定修复](../tickets/T815-binding-audit-fix.md)
- [x] [T816 内容非空回归断言](../tickets/T816-binding-audit-tests.md)
