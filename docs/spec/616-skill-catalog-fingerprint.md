# 616 — 技能目录清单指纹

> 借鉴：[sigstore/cosign](https://github.com/sigstore/cosign) 清单/摘要思想（spec 175 工具指纹的 skills 镜像）。
> 来源：F 会话第 17 轮 = effort #600 / [T882](../../.wayfinder/tickets/T882-skill-fingerprint-shape.md) / [T883](../../.wayfinder/tickets/T883-skill-fingerprint-verify.md) / impl 469。

## 背景

工具目录有 SHA-256 指纹与漂移对账（175/201）；技能目录（yml/DB 动态源）无对应面——技能描述被改、权限被扩、条目被增删都不可对账不可审计。

## 目标

`SkillCatalogFingerprint`：per 技能 sha256 指纹表 + 整体摘要 + 三分类 diff。

## 非目标

- 漂移看门狗接线（175→201 先例：先原语后接线，雾区）。
- 不含技能资源文件内容哈希（目录面先行）。

## 设计

- 契约面 = name + description + allowedTools（description 是模型选择依据属契约——与 175 的工具 description 不计差异诚实入档；权限面必须显形）。
- 输入序不敏感（name 排序后哈希）。

## 测试

4 用例：稳定/序不敏感、三分类、权限变更 CHANGED、null 空安全。

## 兼容性

纯增量新类。
