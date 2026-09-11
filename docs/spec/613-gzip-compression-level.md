# 613 — gzip 导出压缩档位

> 借鉴：[nginx](https://github.com/nginx/nginx) `gzip_comp_level`——压缩档位 = CPU/体积旋钮。
> 来源：F 会话第 14 轮 = effort #600 / [T876](../../.wayfinder/tickets/T876-gzip-level-shape.md) / [T877](../../.wayfinder/tickets/T877-gzip-level-verify.md) / impl 466。

## 背景

观测 JSONL 的三个 gzip 导出面（109 全量 / 67 增量 / 119 单会话）固定 JDK 缺省档——大体量热导出无法省 CPU，冷归档无法求极限体积。

## 目标

三个导出各增 `compressionLevel` 重载（-1 默认 / [0,9]）；无参重载零变化。

## 非目标

- 不接 yml（API 调用面，档位随调用）。
- 不做流式断点续压。

## 设计

匿名子类 `def.setLevel`（protected 唯通路）；越界 fail-fast；档位语义入 Javadoc（Deflater 0..9）。

## 测试

3 用例：9≤1 体积不劣化+内容一致 / 缺省兼容 / 越界拒绝。

## 兼容性

纯增量重载；既有调用零变化。
