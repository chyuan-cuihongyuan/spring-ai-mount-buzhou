# Spec 21 — 配置正规化与质量基建（mechanisms）

> effort #5（T91–T95 / impl-66~70）。

## 配置正规化（T91 / impl-66）

- **by-design 保留**：各机制模块的 `ConfigMaps.sub(env) → Module.fromYml(map)` 契约——模块自有
  配置解析（Builder 形态、编程式路径共用）。已知代价：map 形态键无 IDE 提示，由
  docs/config-reference 全键表补全。
- **正规化缺口（已修）**：auto-config 层散键直读转 `@ConfigurationProperties` record——
  `BuzhouSkillsProperties`（enabled / db-enabled）与 `BuzhouMcpProperties`
  （enabled / dangerous-tool-patterns / shutdown-budget，compact 归一 + fail-fast）。
- guard / tools 无散键直读（结构化解析器 / fromYml map），确认无需改。
- 键名零变化（无迁移、无废弃告警）。

## 供应链安全（T92 / impl-67）

- **SBOM**：CycloneDX（json+xml 双出）——cyclonedx-maven-plugin 为**构建插件注记**（CDX 标准
  格式、仅构建期、不进 runtime classpath）。挂 root pom `supply-chain` profile
  （`makeAggregateBom` @ package），日常构建零开销；supply-chain workflow 产出上传。
- **OWASP dependency-check 观测档**：weekly `supply-chain.yml`，`|| true` 不卡门（对齐
  spotbugs 先例）；NVD key 经 `secrets.NVD_API_KEY` 可选（无 key 降级慢速，文档明示）。
- **Dependabot**：maven（weekly，limit 5）+ github-actions（weekly）；小版本聚合分组。
- SBOM 随 release 发布：M1 不做（release 链不碰；tag 触发上传留 fog）。

## 性能基准（T93 / impl-68）

- **形态**：不引 JMH——`examples` 模块 `PerfBaselineTest`（`@Tag("perf")`）自写 mini-harness
  （warmup + 计时 + P50/P95），目标是**粗粒度回归哨兵**。
- **CI 分层**：root pom surefire 默认 `excludedGroups=perf`（`surefire.excluded.groups` 属性），
  日常零开销；`perf-nightly` workflow（weekly + manual）`-Dgroups=perf` 激活并归档报告工件。
- **场景**：微压缩吞吐（msgs/s）/ 100 轮会话端到端每轮 wall time（零延迟模型度量 harness
  自身开销）/ 存储 append+load-all round-trip。
- **阈值**：10 倍宽幅硬顶哨兵（防环境噪声）；首轮基线落档 `docs/perf/baseline.md`
  （2026-08-15 实测：每轮 P95 0.55ms、微压缩 ~1.8M msgs/s）。
- **解读规则**：跨机绝对值不可比、只看同机趋势、越顶=人工 profiling 不许调阈值。

## 红队数值化（T94 / impl-69）

- （待 T94 决议后补）

## 质量门卡线（T95 / impl-70）

- **覆盖率硬门**：root pom jacoco `check` 执行（BUNDLE / LINE / COVEREDRATIO ≥ **0.70 统一线**，
  phase=verify，本地与 CI 同门槛）。基线：2026-08-15 实测 13 机制模块 77.1%–90.9%（最紧留
  7pp 余量）。豁免：BOM / starter / examples 经 `jacoco.check.skip=true`（无代码 / 纯聚合 /
  演示模块）。越线处理 = 补测试或局部降线+注记，禁止调阈值了事。
- **SpotBugs 硬门**：nightly `threshold=High`（首条 High 即失败）；Medium 及以下归档观测。
  排除纪律：必须 `spotbugs-exclude.xml` + issue 注记（当前无排除清单）。


## effort #7 增补：配置元数据补全（T123 / impl-98）

- core：`buzhou.webhook.outbox-capacity`（默认 10_000）、`buzhou.tools.result-limit-chars`
  （默认 20_000）与 `result-limit-overrides`（Map）；`buzhou.webhook.queue-capacity`
  标记 **deprecated**（warning 级，replacement=outbox-capacity——spec 24 语义）。
- resilience：`circuit.backoff-cap`（默认 8，spec 25）、
  `circuit.half-open-success-threshold`（默认 1，spec 35 §A）。
- skills：新建元数据文件（enabled / db-enabled / catalog-max-entries 默认 64（spec 35 §B）/
  catalog-cache-ttl 默认 30s）。
- IDE 提示/默认值/废弃告警三面齐备（additional-spring-configuration-metadata）。
