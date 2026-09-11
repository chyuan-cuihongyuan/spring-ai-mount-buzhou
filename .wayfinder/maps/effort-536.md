# Wayfinder Map — Buzhou 流式回复秘密扫描（effort #536，E 会话第 36 轮）

> E 会话第 36 轮（400 秘密扫描第四缝 + 500 SPI 第二消费者组合证明轮）。
> 勘察：秘密三缝（400 输入/出站参数/工具结果）+ 回复 PII 流（500）——
> 模型**回复出站流**里的秘密（复读上下文密钥/生成示例密钥）无缝。

## Destination

`guard.secret.SecretScanStreamHook`（order 76——PII 流 75 后）：滑动窗口
缓冲（默认 128）+占位符不拆分+flush 排空（528 族同构——算法同构独立
实现，安全域互不依赖）；复用 SecretScanner（7 型）；命中计数复用
buzhou.guard.secret.redactions + SecretHitStats OUTPUT。GuardModule
secrets.stream-redaction（默认关）。

## Notes

- 号段：spec 536 / T825–826 / impl-438。
- 证明 500 StreamTextFilter SPI 组合性（guard 双 hook 各供 filter）。

## Out of scope

- 会话历史回溯；跨域组合排序策略（hook 序天然组合）。

## Tickets

- [x] [T825 窗口流式秘密扫描](../tickets/T825-secret-stream-hook.md)
- [x] [T826 yml secrets.stream-redaction](../tickets/T826-secret-stream-assembly.md)
