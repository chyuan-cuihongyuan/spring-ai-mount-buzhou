# effort #843 — 证据引用失效率读数

- 会话：H 会话 800 系第 44 轮 ｜ spec [843](../../../docs/spec/843-evidence-ref-validity.md) ｜ 票 [T1187](../tickets/T1187-evidence-ref-validity.md)/[T1188](../tickets/T1188-evidence-ref-validity-verify.md) ｜ impl596
- 借鉴：S3 presigned URL 时限校验思想（引用目标失效=断链）

## 勘察（排重）

- EvidenceRefLedger：包私有引用账（acquire/release）——无失效率对账。
- StoreFsck/ReadIntegrity：完整性校验（内容维）——引用断链维缺位。
- grep -i `ref.*valid|dangling`：无命中。

## 决定

`EvidenceRefValidity`（spill，纯函数）：audit(referencedUris, existence)——失效率=失效/总数+失效样本典序封顶 16；null/空白 URI 忽略；空集空真 0；existence 谓词注入（存在语义归调用方——文件/句柄均可）；null 谓词 fail-fast。

## 测试

失效率 1/3+样本/封顶 16 典序+全失效 1.0/脏 URI 三形态+空真——3 例全绿。

## 诚实边界

existence 语义归调用方（本类不碰文件系统——纯函数纪律）；样本封顶非全量（明细归调用方全列）；时点对账（判定即刻语义）。
