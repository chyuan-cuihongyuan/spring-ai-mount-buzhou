---
id: T65
title: 横切 · 配置校验、元数据与默认值安全化
type: grilling
status: closed
assignee: ""
blocked-by: [T55]
created: 2026-08-14
---

## Question

配置面如何达到 Spring 库标准？需裁决：① 硬编码项全部可配化（并发 8、toolTimeout 60s、LEASE_TTL 90s、loopTimeout、turnPermits——进 BuzhouCoreProperties 及各机制 properties）；② JSR-303 校验引入（@Validated + @Min/@Max，store.type 拼错 fail-fast 的实现——封闭枚举校验）；③ additional-spring-configuration-metadata.json 全量补齐（IDE 提示/默认值）；④ FailureAnalyzer 形状（哪些启动失败模式值得翻译）；⑤ 不安全默认值修正清单（spill root-dir 默认 CWD→独立目录、snapshot-ttl=0、hot-tail maxInlineChars=0、guard 防御默认关→至少 WARN 提示、dialect H2 兜底→显式必填或探测）与兼容性约束（默认值变更不破坏既有契约的原则线）。

## Resolution

**ratify（用户常设授权，可推翻）→ spec 13 §cross-12**：全部硬编码项入 properties（并发/toolTimeout/leaseTtl/loopTimeout/eventDispatch/inMemory/retention/spill 配额/sandbox/policy 刷新）；jakarta.validation @Validated + @Min/@Max + store.type 封闭枚举 fail-fast；全量 additional-spring-configuration-metadata.json；FailureAnalyzer（store 装配/约束冲突）；默认值修正带迁移注记（spill root-dir 独立临时目录、snapshot-ttl PT168H、hot-tail 65536、dialect 缺省 DatabaseMetaData 自动探测）；兼容性原则线——新增默认只影响此前即不安全的路径。
