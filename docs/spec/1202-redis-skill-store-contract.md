# 1202 — skills RedisSkillStore 契约补测（R3）

> 来源：K 会话第 3 轮 = effort #1202（[T1809](../../.wayfinder/tickets/T1809-redis-skill-store-contract-shape.md) / [T1810](../../.wayfinder/tickets/T1810-redis-skill-store-contract-verify.md) / impl 905）。方法论：**consumer-driven contract testing（Pact 思想）**——同一组契约断言驱动全部持久化实现；「契约缺席的实现 = 未被验收的实现」。

## Problem Statement

`SkillStore` 的契约基类（`AbstractSkillStoreContractTest`）已覆盖 InMemory 与 JDBC 两实现，但 Redis 实现 `RedisSkillStore`（cov=0 / mis=26）从未接入——它的 hash/set/JSON round-trip 路径、乐观锁冲突、级联删除均无任何测试。R1 零覆盖审计漏扫 skills 模块致其漏出（审计结论未逐模块罗列的流程漏洞）。「同组行为断言覆盖全部实现」的契约范式在其上断链：Redis 实现是三实现中唯一经真实网络与序列化路径的，恰是最需要契约验收的一个。

## 目标

- **契约接入**：`RedisSkillStoreContractTest extends AbstractSkillStoreContractTest`，四个契约用例（round-trip / 草稿对运行时不可见 / 乐观锁拒绝旧版本 / 资源 CRUD 与级联删除）经真实 redis:7-alpine 容器执行。
- **持久语义加验**（RedisSessionIndexContractTest 同款超额面）：新 store 实例（模拟重启）同 Redis 可见全部行——JSON 序列化往返经真实网络，验证重启存活。
- **隔离纪律**：`@AfterEach` 用 store 公共 API 清场（findAll → deleteByName 级联清资源 hash），不复制私有键前缀；容器 static 共享 + 前缀无关（键空间固定，靠清场隔离）。

## 实现决策

- 门控沿 store-redis 同款：`@Testcontainers(disabledWithoutDocker = true)` + `GenericContainer("redis:7-alpine")`（与 RedisSessionIndexContractTest 字面一致）。
- 接线：`LettuceConnectionFactory(host, port)` + `afterPropertiesSet()` → `StringRedisTemplate`（spring-data-redis 4.1.1 构造器实证存在）；不引 spring-boot-test 装配——直接构造最小化。
- skills pom 补 `org.testcontainers:testcontainers-junit-jupiter`（test scope，版本走根 POM `testcontainers.version` 管理——store-redis 同款，非新引第三方）。
- 不选手写 fake `StringRedisTemplate`：三 ops 接口 60+ 方法全 stub 是 Mockito 的手工复刻，且测不到真实 JSON/网络路径。

## 测试决策

- 契约断言只测外部行为（save/find/version 冲突/级联删除的输入→输出），不测 Redis 命令序列——契约基类已定，本切片只供给实现绑定与清场。
- 无 Docker 验证口径：本机无 Docker → 契约按设计 skip（不 fail），本地证据 = 编译绿 + 测试收集 + skip 数；行为验证声明限定「Docker 在场（CI）」——store-redis/store-jdbc 既有口径，诚实入档。
- 先例：RedisSessionIndexContractTest（门控 + 持久语义 + API 清场）、JdbcSkillStoreContractTest（同基类接入）。

## 兼容性

skills pom 补 test 依赖 + 新增一个测试文件：主代码零变化、公共 API 面零变化、既有测试零改动。

## Out of Scope

- 不给 RedisSkillStore 加命名空间/前缀配置（现键空间固定是设计现状，契约经 API 清场隔离已足）。
- 不做手写 fake 双轨（决策见实现决策第 4 条）。
- 不在本轮追 core 低覆盖批次（待复扫证据归 R4）。

## Further Notes

- R1 审计遗漏修正入档：零覆盖审计结论必须逐模块罗列——「其余无零覆盖类」的一笔带过是本次漏出的直接原因，已记入 K 会话 map 与 T1809。
- 本轮后 skills 模块三持久化实现（InMemory/JDBC/Redis）全部过同一契约——「契约范式无断链」恢复。
