---
Type: task
Status: open
---
## Question

SessionIndexStore 契约测试矩阵（T109 补强）：三实现（内存/JDBC/Redis）目前各自行为测试，无共享契约——语义漂移不可检。是否建 AbstractSessionIndexContractTest 并接入三实现（H2 直跑 / Redis Testcontainers 守卫）？

## Resolution

AFK 自决：是。建共享契约套件（upsert 幂等收敛/get/list 过滤组合与 lastActive 倒序/tag 精确/delete 幂等/重启持久〔JDBC: 新 store 实例同数据源；Redis: 同容器；内存: 重启重建语义豁免为「空索引 + 随活动重建」断言〕），三实现接入；实现私有细节留在各自原有测试。产 spec 33 §A + impl-87。
