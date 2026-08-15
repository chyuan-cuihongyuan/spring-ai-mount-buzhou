# impl-103 — release SBOM 附着

**What to build:** CycloneDX 聚合 BOM（json+xml）随 GitHub Release 发布为附件。

**Blocked by:** None

**Status:** done

- [x] release.yml 增两步：-Psupply-chain package 生成 BOM + softprops/action-gh-release 附着
- [x] RELEASING.md 检查单同步（附件核对项）
- [x] workflow YAML 校验

## Done

commit：见 git log（impl-103）。
