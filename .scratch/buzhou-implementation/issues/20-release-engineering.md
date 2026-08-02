# 20 — 发布工程化

**What to build:** Central Portal 发布通道就绪：central-publishing-maven-plugin+maven-gpg-plugin 配置进父 POM；发布 workflow（tag 触发、复用 CI verify）；版本号策略与发布 checklist 文档化；dry-run 发布到 Central Portal staging 验证签名与构件完整性。

**Blocked by:** 01

**Status:** ready-for-agent

- [ ] `mvn -P release verify` 本地出齐签名构件
- [ ] 发布 workflow 在 tag 推送后跑通至 staging（人工首发按 ticket 20 checklist）
- [ ] BOM 统一版本、全模块同版本演进有校验
