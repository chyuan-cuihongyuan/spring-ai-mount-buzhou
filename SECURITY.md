# 安全策略（Security Policy）

## 报告安全漏洞

如果你发现 Buzhou 存在安全漏洞，**请不要公开开 Issue**。请通过以下任一方式私下报告，以便我们在发布修复前评估问题：

- **GitHub Private Security Advisory**：前往
  [Security 标签页](https://github.com/chyuan-cuihongyuan/spring-ai-mount-buzhou/security/advisories/new)
  提交私密报告（推荐）；
- 或联系维护者：[github.com/chyuan-cuihongyuan](https://github.com/chyuan-cuihongyuan)。

报告时请尽量包含：受影响版本、复现步骤、影响范围与（如有可能）修复建议。我们会在收到报告后尽快确认，并与你协调披露时间。

## 支持的版本

Buzhou 目前处于早期开发阶段（`0.x`），仅对最新发布版本提供安全修复。

| 版本 | 是否支持安全更新 |
|------|------------------|
| 最新 `0.x` 发布版 | ✅ |
| 更早版本 | ❌ |

## 适用范围

安全漏洞通常指：在 Buzhou 代码库范围内可被利用的非预期行为（如护栏绕过、敏感信息泄露、持久化层的越权访问等）。一般的 bug 或使用问题请走 [Issue](https://github.com/chyuan-cuihongyuan/spring-ai-mount-buzhou/issues) 而非安全报告流程。

Buzhou 作为中间层，**不替代** 你在业务侧对工具、模型输出、外部数据源所做的安全校验。生产部署请遵循 Spring AI 与 Spring Boot 的安全最佳实践。
