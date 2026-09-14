---
id: T1809
title: skills RedisSkillStore 零覆盖补测形态（契约接入 + R1 审计遗漏修正）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 3 轮：buzhou-skills `RedisSkillStore`（cov=0 / mis=26）为何漏出？补测形态如何裁决——Testcontainers 契约测试还是手写 fake？无 Docker 环境的验证口径是什么？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 3 轮 = effort #1202 / spec 1202 / impl 905）：

1. **R1 审计遗漏修正（诚实入档）**：R1 声称「审计覆盖全部 16 模块」，但其无零覆盖结论清单（memory/tools/observability/observe-otel/observe-dashboard/store-jdbc/store-redis）未含 skills——skills 报告实际存在 `RedisSkillStore` cov=0 / mis=26。教训：零覆盖审计结论必须逐模块罗列、不许「其余无」一笔带过。
2. **形态 = Testcontainers 契约接入**（store-redis `RedisSessionIndexContractTest` 同款门控 `@Testcontainers(disabledWithoutDocker = true)` + `redis:7-alpine`）：`RedisSkillStoreContractTest extends AbstractSkillStoreContractTest`——同一组行为断言覆盖全部三实现（InMemory/JDBC/Redis）是既定范式（契约基类 Javadoc 明文）。不选手写 fake `StringRedisTemplate`：opsForValue/Set/Hash 三接口合计 60+ 方法全 stub 是 Mockito 的手工复刻，成本高且测的是 fake 不是 Redis 序列化路径（JSON round-trip 真实性是该实现的核心风险）。
3. **依赖**：skills pom 补 `org.testcontainers:testcontainers-junit-jupiter`（test scope，版本走根 POM `${testcontainers.version}` 管理，store-redis 同款——非新引第三方）。
4. **无 Docker 验证口径（fog 物化）**：本机无 Docker → 契约按设计跳过，本地证据 = 编译绿 + 测试收集 + skip；行为验证声明限定「Docker 在场（CI）」，与 store-redis/store-jdbc 既有口径一致。isolation 用 store 公共 API 清场（`findAll().forEach(deleteByName)`，级联清资源 hash），不复制私有键前缀。
5. **边界**：不改 RedisSkillStore 主代码；补测显形缺陷单列票。
