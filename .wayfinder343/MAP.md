# Wayfinder Map — Buzhou 生效配置自描述端点（effort #343，C 会话第 44 轮）

> C 会话第 44 轮。buzhou 机制已 40+ 个、开关散布几十个前缀——运维答
> 「这台实例到底开了什么、阈值是多少」要翻 yml+env+代码默认三层。
> Spring Boot actuator /configprops 的思想：运行时自描述生效配置；
> 加上密钥掩码纪律（333 master-key 绝不能进端点）。

## Destination

`BuzhouConfigSnapshotEndpoint`（`/actuator/buzhou-config`）：枚举
Environment 中全部 `buzhou.*` 生效属性（yml/env/命令行全覆盖——真实
生效值而非声明值），密钥类键（key/secret/password/token 匹配）值掩码
`***`；无 buzhou.* 属性 → 空 map 诚实；只读。

## Notes

- 号段：spec 343 / T677–T678 / impl-366。
- 借鉴源：Spring Boot actuator configprops（+sanitization 掩码纪律）。
- 纪律：掩码匹配保守（宽匹配宁掩勿漏）；端点挂 actuator 条件配置类
> （332 同位）。

## Decisions so far

- 数据源 = Environment 直读（含 env 覆盖与命令行——比 configprops 的
  bean 视角更贴「实际生效」且零 bean 依赖）。
- 掩码判定对整键小写匹配子串 key/secret/password/token/credential。

## Out of scope

- 修改操作（只读）；历史/漂移对比（320 族）；分 bean 分组视图。

## Tickets

- [x] [T677 端点+掩码](tickets/T677-config-endpoint.md)
- [x] [T678 装配+收口](tickets/T678-config-assembly.md)
