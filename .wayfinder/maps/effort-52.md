# Wayfinder Map — Buzhou 配置体检 doctor（effort #52，50 轮自迭代第 17 轮）

> effort #52，延续 #51（T343–T344 / impl-237）。主线：**#51 插曲的直接产品化**——
> 本地仓库 jar 与源码配置面漂移导致矩阵假失败；配置类问题的第一来源是「拼错键
> 静默失效」与「值域越界」。Spring Shell doctor 思想。

## Destination

`ConfigDoctor`：classpath 各模块 metadata json 聚合键宇宙；`examine(Map)` 把实际
buzhou.* 键值面对照——未知键 WARN（编辑距离 ≤2 最近邻建议「是否想写 X？」）、
声明类型值域不可解析 ERROR（Boolean 严格白名单——parseBoolean 恒 false 陷阱）、
合法 INFO 计数；报告有界（64 条防刷爆）+ 单行 summary；`examine(Environment)`
聚合入口（只枚举 buzhou.*）；autoconfig `buzhou.config-doctor.enabled`（默认关）
就绪事件日志一次。只读不写。

## Notes

- 借鉴：Spring Shell doctor / Spring Boot diagnostics；metadata json 是键宇宙的
  既有事实源（矩阵测试同法聚合）。

## Decisions so far

- Boolean 值域走严格白名单（Boolean.parseBoolean 对任意输入 false 不抛——陷阱）。
- 值内容不进报告（可能敏感——只报「不可解析」）。

## Not yet specified

- 矛盾组合检测（如 drain 期间 auto-resume——跨键语义规则表）；健康端点暴露
  report；doctor 面进 starter 装配。

## Out of scope

- 沿用 #7–#51；配置修改建议的自动应用（只读纪律）。

## Tickets

- [x] [T347 ConfigDoctor 静态面 + Environment 聚合](../tickets/T347-config-doctor.md)（impl-238）
- [x] [T348 6 例红队（近邻/无近邻/值域/干净有界/env 聚合/levenshtein）+ 收口](../tickets/T348-doctor-close.md)
