---
Type: task
Status: closed
---
## Question

`/actuator/buzhou-probes` 端点（三裁决 + failing + 成员清单）+ yml 面
`buzhou.health.probes.{liveness,startup}-mechanisms` + README/快照收口。

## Resolution

done（2026-09-04）：impl-355；端点挂既有 actuator 条件配置类
（BuzhouEndpointConfiguration），属性类 BuzhouProbeProperties（机制
引用端点装配期 fail-fast——312 同口径）；端点 payload {类: {status,
failing, mechanisms}}。装配三用例 + 端点两用例绿；快照 regenerate +3
型；README 纵深 IV 加行、覆盖门绿。
