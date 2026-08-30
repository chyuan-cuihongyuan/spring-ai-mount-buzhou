# Wayfinder Map — Buzhou 配置体检健康段（effort #69，50 轮自迭代第 34 轮）

> effort #69，延续 #68（T391–T392 / impl-253）。主线：**spec 91 fog 项「健康端点
> 暴露 report」**——doctor 报告只在启动日志一闪而过，运维回看不便。

## Destination

`ConfigDoctorHealth implements BuzhouHealth, ApplicationListener<ApplicationReadyEvent>`
（mechanism=config-doctor）：就绪事件跑一次 ConfigDoctor（发现走日志）+ 报告缓存
（AtomicReference）；状态就绪前 UNKNOWN（pending）→ 就绪后 UP（错误数在 details，
DOWN 留给核心职能——error-signatures 同纪律）；details = errors/warnings/
checkedKeys（明细留日志——健康详情有界）；autoconfig 原 listener bean 替换为本
复合 bean（同键 buzhou.config-doctor.enabled 默认关）。

## Notes

- 借鉴：健康段家族第六员；listener+health 复合 bean（Spring Cloud Config 同款
  一次性体检缓存形态）。

## Decisions so far

- 体检一次缓存（不每查询重跑——键宇宙 classpath 扫描是一次性成本）。

## Not yet specified

- 重跑触发面（手动 refresh——运维需求后议）。

## Out of scope

- 沿用 #7–#68。

## Tickets

- [x] [T393 ConfigDoctorHealth 复合 bean 替换装配](tickets/T395-doctor-health.md)（impl-254）
- [x] [T394 2 例红队（就绪前 UNKNOWN→UP 缓存/端点聚合）+ 收口](tickets/T396-doctor-health-close.md)
