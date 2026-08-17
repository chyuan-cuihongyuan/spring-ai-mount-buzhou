---
Type: task
Status: closed
---
## Question

配置元数据补全：additional-spring-configuration-metadata.json 三模块已有，effort #6 新键（webhook outbox-capacity、tools result-limit*、circuit backoff-cap、index 装配）无 IDE 提示与默认值文档。补全范围？

## Resolution

AFK 自决：补全四处（core 的 webhook/tools 键；resilience 的 backoff-cap/half-open-success-threshold〔T118 后〕；jdbc/redis 的 index 相关注释；skills catalog-max-entries〔T119 后〕）——每键 description/defaultValues/deprecation（queue-capacity 标 deprecated）。产 impl-98（并入 spec 21 配置面增补节）。
