# Wayfinder Map — Buzhou 会话导出校验和（effort #547，E 会话第 47 轮）

> E 会话第 47 轮（511 密文封缄的明文通道对偶轮；S3 checksum 同思想）。
> 勘察：510 密文封缄护密文通道——**明文导出**（分享给可信方的 JSON）
> 传输/存储衰变无证据：损坏的 JSON 可能恰好仍可解析但内容已变。

## Destination

`session.SessionExportChecksum`（静态原语）：of(exportJson) →
"sha256:<hex>"；verify(exportJson, checksum) → 一致 true / 不一致或
格式不符 false（fail-closed）。明文导出文件旁写校验和，导入前验校。

## Notes

- 号段：spec 547 / T853-854 / impl-449。
- 借鉴源：S3 checksum / 511 同 doctrine。

## Out of scope

- 防蓄意同改（归 510 密文封缄）；签名（防篡改归 510）。

## Tickets

- [x] [T853 校验和原语](../tickets/T853-export-checksum.md)
- [x] [T854 fail-closed 语义](../tickets/T854-export-checksum-verify.md)
