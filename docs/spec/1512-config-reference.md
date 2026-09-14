# 1512 — 配置全键表 config-reference（F9 闭环）

> 来源：M 会话第 14 轮 = effort #1512（impl 1115）。spec 21:9 承诺（design-incompleteness F9）落地。

## 背景

spec 21 承诺 map 形态键「由 docs/config-reference 全键表补全」——文件不存在。

## 目标

- `docs/config-reference.md` 三段式：① @ConfigurationProperties record 组件全表（57 record / 198 组件键：前缀/键/类型/模块）；② fromYml(Map) 契约子键段（4 模块，语义详注指针到模块 spec）；③ Environment 直读键（34 个 getProperty 字面量）；
- camelCase ≡ kebab-case relaxed binding 说明；组件级默认值指针到 compact constructor；中文语义注记声明为后续增量。

## 兼容性

纯文档，零行为变化；再生成口径入档（扫描器逻辑记录于本轮 impl）。
