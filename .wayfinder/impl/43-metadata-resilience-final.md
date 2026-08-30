# 43 — 收口 · 配置元数据 + 韧性矩阵补齐 + 终验

**What to build:** 「库感」收口与全量背书：additional-spring-configuration-metadata.json 全量补齐（默认值 + 枚举 hints）；韧性场景矩阵补齐遗漏项（停机排空/续租 steal/写失败双策略/脏 JSON/熔断半开等跨片场景在 examples 统一回归）；全量 mvn verify 绿 + 机制 Spec 同步 + MAP 落地记录。

**Blocked by:** 42

**Status:** ready-for-agent

- [ ] 四模块 additional-spring-configuration-metadata.json（默认值/枚举 hints/弃用条目占位）
- [ ] examples 韧性回归矩阵统一入口（FaultInjecting 驱动，覆盖 spec 13 列举场景中未随片落地的）
- [ ] mvn -B clean verify 全模块绿（含 Testcontainers gated）
- [ ] 机制 Spec 同步：05（停机/Deadline/租约）、09（存储运维节）、01（熔断半开）、07（审计/密钥/policy/限额）、00 概览提及
- [ ] `.wayfinder/impl/README.md` 索引 + `maps/effort-03.md` 落地记录
