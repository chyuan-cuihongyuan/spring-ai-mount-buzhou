---
Type: task
Status: open
blocked-by:
---
## Question

供应链安全怎么做？现状：CI 只有 CodeQL+SpotBugs 观测；无 SBOM/依赖漏洞扫描/Dependabot。决策点：SBOM 格式与工具（CycloneDX maven 插件，构建期生成，注记非 10K★ 但为构建插件）、OWASP dependency-check 观测档（NVD API key 问题如何处理——无 key 时降级）、 Dependabot 配置范围（maven+actions）、SBOM 是否随 release 发布。产出 spec 21 增量 + impl 67。
