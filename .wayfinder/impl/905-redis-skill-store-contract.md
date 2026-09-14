# 905 — skills RedisSkillStore 契约补测（R3）

**What to build:** `RedisSkillStoreContractTest`——Redis 实现接入 SkillStore 契约基类（四个契约用例经真实 redis:7-alpine 容器），加重启存活语义加验；无 Docker 按设计跳过。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] skills pom 补 `testcontainers-junit-jupiter`（test scope，根 POM 版本管理）
- [x] RedisSkillStoreContractTest：契约四用例 + rowsSurviveNewStoreInstance + @AfterEach API 清场（findAll→deleteByName 级联，不复制私有键前缀）+ @AfterAll 工厂销毁
- [x] spec 1202 + README 行
- [x] 验证：skills 134 用例 0 失败；契约 5 用例无 Docker 按设计 skip（收集完整）；spring-data-redis 4.1.1 LettuceConnectionFactory(host, port) 构造器 javap 实证

## Done

验证：skills 全量绿（134/0/0），契约按设计 skip、行为面 CI 覆盖（T1810 口径）。R1 审计漏扫 skills 已入 map 修正。commit 见本轮 `test(skills)` 提交。
