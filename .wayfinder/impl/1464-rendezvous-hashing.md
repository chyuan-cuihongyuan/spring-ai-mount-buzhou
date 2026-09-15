# impl 1464 — RendezvousHashing 键归属指派（R63 = effort #1862 / spec 1862 / T2925-T2926）

**What**：`RendezvousHashing`（core/cache 静态纯函数）——assign（确定性
混合评分取最高，并列字典序）+ assignAll（确定性可回放）；空键/空节点/
空白节点/null fail-fast。

**Why**：HRW/Rendezvous 思想——取模哈希节点增删全量重排、一致性环要虚
节点；HRW 最小迁移（摘节点只动原属它的键）+ 免环管理 + 确定性三全。

**Verify**：`RendezvousHashingTest` 4 用例全绿（含 200 键最小迁移全检）。

**Status**：done（2026-09-16）
