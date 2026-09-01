# Wayfinder Map — Buzhou 事件 schema yml 声明（effort #307，C 会话第 8 轮）

> C 会话第 8 轮。spec 209 的 `EventSchemaChecker` 纯编程构造（Map 注入）——
> 事件契约声明无 yml 面（fog 227「事件 schema 声明 yml 化」项）。

## Destination

`buzhou.webhook.schema.required-keys.<type>=k1,k2` + `fail-open`（观察模式）
yml 声明事件契约；装配面自动包装 forwarder（缺键 fail-closed 丢弃 / 违规
计数）；全局挂点去重——被 checker 包装的 forwarder 不重复直挂（防双投）。

## Notes

- 号段：spec 307 / T605–T606 / impl-330。
- 借鉴：JSON Schema required（209 原始来源——声明面收尾）。

## Decisions so far

- 未声明类型放行（209 open-world 不变）；required-keys 空 = 不装配 checker。
- checker bean 用 NullBean 语义（空声明返回 null → 类型收集自动跳过）。

## Out of scope

- 信封层 schema（20 已有）；类型注册制（closed-world）。

## Tickets

- [x] [T605 schema 属性组 + checker 装配（NullBean）+ 挂点去重](tickets/T605-schema-yml.md)（impl-330）
- [x] [T606 yml 绑定 + fail-closed/fail-open + 去重回归](tickets/T606-schema-close.md)（impl-330）
