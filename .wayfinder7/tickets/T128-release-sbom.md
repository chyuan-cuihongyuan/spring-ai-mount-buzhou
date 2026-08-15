---
Type: task
Status: open
---
## Question

发布流程 SBOM 附着（T92 fog）：release.yml 是否上传 CycloneDX SBOM 产物到 GitHub Release？

## Resolution

AFK 自决：是。supply-chain profile 已产 SBOM——release workflow 增 job：checkout→`mvn -Psupply-chain package` 产 bom→softprops/action-attach（或 gh release upload）附 SBOM artifact；workflow 只在 tag push 触发现有路径上追加。产 impl-103（RELEASING.md 检查单同步）。
