# 67 — SBOM + 依赖漏洞扫描 + Dependabot（T92 决策落地）

**What to build:** root pom `supply-chain` profile（CycloneDX makeAggregateBom）+ `.github/workflows/supply-chain.yml`（SBOM 工件 + dependency-check 观测）+ `.github/dependabot.yml`。

**Blocked by:** None.

**Status:** done

- [ ] root pom：supply-chain profile 挂 cyclonedx-maven-plugin（json+xml，aggregated，package 阶段）
- [ ] supply-chain.yml：weekly + manual —— SBOM 生成上传工件 + OWASP dependency-check 观测（|| true，NVD key 可选 secret）
- [ ] dependabot.yml：maven + github-actions，weekly，小版本聚合
- [ ] 本地烟测：`mvn -Psupply-chain -DskipTests package` 产出 bom.json/xml（不进 commit）

## Done

验证：本地烟测 `./mvnw -Psupply-chain -DskipTests package` 产出 root 聚合 `target/bom.json + bom.xml`（CycloneDX 双格式）；workflow/dependabot YAML 语法落盘。
落地：root pom `supply-chain` profile（cyclonedx-maven-plugin 2.9.1 makeAggregateBom @ package，构建插件注记，日常零开销）；`.github/workflows/supply-chain.yml`（weekly+manual：SBOM 上传工件 + OWASP dependency-check 观测档 continue-on-error + NVD key secret 可选 + 报告工件）；`.github/dependabot.yml`（maven+actions weekly，小版本聚合分组）。SBOM 随 release 发布留 fog（release 链不碰）。
