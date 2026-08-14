# 67 — SBOM + 依赖漏洞扫描 + Dependabot（T92 决策落地）

**What to build:** root pom `supply-chain` profile（CycloneDX makeAggregateBom）+ `.github/workflows/supply-chain.yml`（SBOM 工件 + dependency-check 观测）+ `.github/dependabot.yml`。

**Blocked by:** None.

**Status:** open

- [ ] root pom：supply-chain profile 挂 cyclonedx-maven-plugin（json+xml，aggregated，package 阶段）
- [ ] supply-chain.yml：weekly + manual —— SBOM 生成上传工件 + OWASP dependency-check 观测（|| true，NVD key 可选 secret）
- [ ] dependabot.yml：maven + github-actions，weekly，小版本聚合
- [ ] 本地烟测：`mvn -Psupply-chain -DskipTests package` 产出 bom.json/xml（不进 commit）

## Done

（待填）
