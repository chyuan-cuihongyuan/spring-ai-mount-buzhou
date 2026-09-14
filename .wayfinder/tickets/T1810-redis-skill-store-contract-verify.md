---
id: T1810
title: skills RedisSkillStore 契约测试验证
type: task
status: closed
assignee: zcode-k
blocked-by:
  - T1809
created: 2026-09-15
---

## Question

R3 补测后：RedisSkillStoreContractTest 是否编译绿 + 收集完整？无 Docker 环境是否按设计跳过（skip 而非 fail）？skills 模块其余测试是否绿？

## Resolution

**用户常设授权 AFK（可推翻）**

验证结论（2026-09-15，skills 全量 test）：

1. **收集完整**：RedisSkillStoreContractTest 5 用例（契约 4 + 重启存活 1）全部被 surefire 收集执行——Testcontainers 门控生效路径为 skip 而非 fail。
2. **无 Docker 口径符合设计**：本机 Docker CLI 在场但守护进程未运行（环境惯例即无 Docker）——Testcontainers 探测失败 → `disabledWithoutDocker` 跳过，日志显形 `Could not find a valid Docker environment` 后 5 skip / 0 fail / 0 error；行为面验证声明限定 Docker 在场（CI）——与 store-redis/store-jdbc 既有口径一致，诚实入档。
3. **skills 模块全绿**：134 用例 0 失败 0 错误（含新收集的 5 skip）。
4. 主代码零变化；pom 仅补 testcontainers-junit-jupiter（test scope，根 POM 版本管理，store-redis 同款先例）。
5. 残余风险如实声明：容器路径的运行时正确性（模板接线 / JSON round-trip / 级联删除）本地未验证，由 CI Docker 环境跑真——决策票 T1809 第 4 条已明示该口径。
