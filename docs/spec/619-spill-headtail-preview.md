# 619 — spill 预览头尾语义

> 借鉴：[BurntSushi/ripgrep](https://github.com/BurntSushi/ripgrep) context 语义——匹配行前后上下文优先可见。
> 来源：F 会话第 20 轮 = effort #600 / [T888](../../.wayfinder/tickets/T888-headtail-preview-shape.md) / [T889](../../.wayfinder/tickets/T889-headtail-preview-verify.md) / impl 472。

## 背景

spill 预览（spec 02，默认 2048 字符）纯头截断——工具大结果的关键信息常在尾部（汇总行/结论/总计数），模型只见开头就要决定是否回读。

## 目标

截断预览 = 头 3/4 + 省略标注 + 尾 1/4。

## 非目标

- 不改 JSON 数组分页预览（readPage 首页带 totalCount 已是全貌口径）。
- 不改 previewChars 配置语义。

## 设计

标注文案含 omitted 字符数与 read_range 回读指引（模型可依此决策）；预算量级不变。

## 测试

3 用例：头尾含首末行+标注 / 短内容原样 / 数组路径不受影响；spill 全模块零回归。

## 兼容性

截断路径的预览内容变化（行为改进——既有测试零回归佐证无人依赖纯头形状）。
