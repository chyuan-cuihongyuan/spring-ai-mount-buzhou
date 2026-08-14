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

- （待 T95 决议后补）
