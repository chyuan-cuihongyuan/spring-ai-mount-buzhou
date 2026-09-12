# 712 — JSONL 轮转旧档 gzip 压缩

> 来源：G 会话第 13 轮 = effort #712（借鉴 logrotate `compress` + `delaycompress`）/ [T975](../../.wayfinder/tickets/T975-jsonl-gzip-shape.md) / [T976](../../.wayfinder/tickets/T976-jsonl-gzip-verify.md) / impl 515。

## 背景

RollingJsonlWriter（spec 642）代际 shift 全为明文——默认 64MB×3 ≈ 256MB/文件组的观测明细（JSONL、重复键多、压缩比 ~10:1）盘上全是明文。logrotate 的洞察：旧档压缩（compress）但**最新一代先留明文**（delaycompress）——热查与省盘兼得。

## 目标

- `compressFromGeneration` opt-in（默认 0=关——既有三构造与静态路径行为逐字节不变）：代际 ≥ N 历史档存 gzip（`file.N.gz`）；`file.1` 恒明文（delaycompress——tail/grep 热查不受影响）。
- shift 语义：明文→gzip 线上转码（GZPOutputStream + 删源）；gz→gz 纯 rename；压缩单调（compressed→plain 不可能路径——compressFrom ≥ 2 且代际递增）；最老代清理清两种形态。
- 实例 rotate() 与静态 rotateIfNeeded 同口径（新重载携带 compressFrom；旧签名默认 0 委托）。
- gzip 失败计入 `rotationFailures` best-effort 降级（与轮转失败同通道）；指标复用 `buzhou.jsonl.rotated`。

## 非目标

不做 zstd/xz（JDK 内置 gzip 零依赖）；不做读取端自动解压 API（读侧是外部工具/后续轮）。

## 测试

compressFrom=2 轮转：file.1 明文、file.2.gz 可解且内容=第一代、无 file.2 明文；默认 0 全明文零回归；静态路径同口径；双形态清理。

## 兼容性

opt-in 纯增量；默认逐字节不变。
