# Contributing

感谢关注 Spring AI Mount Buzhou。

## 环境

- JDK 21+
- Maven 3.9+

## 构建与测试

```bash
mvn verify
```

## 提交约定

- 提交信息使用简洁祈使句，建议遵循 Conventional Commits（`feat:` / `fix:` / `docs:` / `refactor:` / `test:`）。
- 一个 PR 只做一件事；行为变更必须带测试。
- 公共 API 变更需在 PR 描述中说明兼容性影响。

## 设计约定

- 领域术语以仓库根目录 `CONTEXT.md` 为准。
- 机制设计以 `docs/spec/` 的 Spec 为准；改机制先改 Spec。

## Issue

- bug 请附：版本、最小复现、期望/实际行为。
- 功能请求请先开 issue 讨论再动代码。
