# 913 — 导出域三件套联动 e2e

> 来源：I 会话第 14 轮 = effort #913（[T1277](../../.wayfinder/tickets/T1277-export-domain-e2e-shape.md) / [T1278](../../.wayfinder/tickets/T1278-export-domain-e2e-verify.md) / impl 666）。G 会话补验轮模式（r34–r43 先例）：机制单测绿 ≠ 编排语义闭环。

## 背景

导出域五组件（710 协商 / 733 双校验和 / 904 审计 / 911 规范化指纹 / 912 diff）各自单测绿，但组件间口径是否互相咬合（911 的键序不敏感性会不会破坏 710 的变更判定？904 的严格拒绝会不会误伤 710 的正常协商产物？）无实证。

## 目标

`ExportDomainE2ETest`（core.session 测试域，纯编排零生产变更）五场景：

1. **键序漂移不误判**：同内容嵌套键序漂移的两份导出——JCS 指纹相同；协商（exportIfChanged）仍判 UNCHANGED；
2. **内容真变正确 EXPORTED**：消息内容变更 → 规范化指纹变 → 协商 EXPORTED 且携带新指纹；
3. **审计与宽松正交**：带未知顶层字段的文档——宽松 fromJson 成功（向后兼容）+ audit strictCompatible=false + fromJsonStrict 拒绝；
4. **diff 口径与协商一致**：两次仅时戳不同的导出——diff identical=true（时戳双口径一致排除）；
5. **diff×指纹联动**：内容变更的两次导出——diff 显形消息桶差异 且 规范化指纹不同（两种口径同判「变了」）。

## 兼容性

纯测试编排轮；零生产代码变更（若实证出缺陷，按 G r39 先例修复实现并在本 spec 补记）。

## 实证结果（e2e 抓到真实联动缺陷——G r39 时刻重现）

场景 1 首跑红灯：JCS 指纹相等但协商判 EXPORTED。根因：`exportIfChanged` 硬编码保序
`contentFingerprint`，与 `sha256-j:` 规范化指纹不成对——口径混用必误判。修复：新增
`exportIfChangedCanonical`（规范化指纹配对协商重载）；既有 `exportIfChanged` 逐字节
不动（既有协商状态兼容）。教训入档：**指纹口径必须与协商口径成对出现，跨口径比较
等于没比。**
