---
Type: task
Status: closed
assignee: zcode
blocked-by:
---
## Question

供应链安全怎么做？现状：CI 只有 CodeQL+SpotBugs 观测；无 SBOM/依赖漏洞扫描/Dependabot。决策点：SBOM 格式与工具（CycloneDX maven 插件，构建期生成，注记非 10K★ 但为构建插件）、OWASP dependency-check 观测档（NVD API key 问题如何处理——无 key 时降级）、 Dependabot 配置范围（maven+actions）、SBOM 是否随 release 发布。产出 spec 21 增量 + impl 67。

## Resolution

AFK 自决（授权同 T81，可推翻）：

1. **SBOM**：CycloneDX（CDX Foundation 标准，format=json+xml 双出）——cyclonedx-maven-plugin **构建插件注记**（非 10K★ 但 CDX 是行业标准格式、插件仅构建期、不进 runtime classpath，符合政策豁免条款）。挂 root pom `supply-chain` profile（`makeAggregateBom` @ package），日常构建零开销；supply-chain workflow 产出并上传工件。
2. **OWASP dependency-check**：**观测档**（对齐 spotbugs 先例：weekly workflow `|| true` 不卡门）——NVD key 经 `secrets.NVD_API_KEY` 可选注入（无 key 降级慢速全量，文档明示）；`failAuditOnCVSS` 不设（观测）。retire/central 仓库兜底数据源默认开。
3. **Dependabot**：`.github/dependabot.yml`——`maven`（根 `/`，weekly，open-pull-requests-limit 5）+ `github-actions`（`/`，weekly）；分组 `buzhou-deps` 小版本聚合（minor/patch），major 单独 PR。
4. **SBOM 随 release**：M1 不动 release.yml（发布链已就绪不碰）；SBOM 经 supply-chain workflow 工件归档 + tag 触发时上传 release assets 留作后续（fog）。
