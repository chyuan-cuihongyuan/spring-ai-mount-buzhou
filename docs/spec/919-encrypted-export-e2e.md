# 919 — 加密导出×审计×指纹联动 e2e

> 来源：I 会话第 20 轮 = effort #919（[T1289](../../.wayfinder/tickets/T1289-encrypted-export-e2e-shape.md) / [T1290](../../.wayfinder/tickets/T1290-encrypted-export-e2e-verify.md) / impl 672）。G 会话补验轮模式（r39 先例）。

## 背景

导出域存在**明文/密文两形态**（510 封缄容器 vs 28 可移植 JSON），904 审计 / 911 规范化指纹均定义在明文形态上。「密文载荷误入明文审计面」「解封产物能否直接走严格导入」「nonce 导致密文不同但内容指纹稳定」三条编排语义无实证。

## 目标

`EncryptedExportE2ETest`（core.session 测试域，纯编排零生产变更）四场景：

1. **密文进明文审计 fail-closed**：sealed 载荷进 `SessionExportAudit.audit` → IllegalArgumentException（密文非明文 JSON——fail-closed 固化）；
2. **seal→open→审计/严格导入全链**：seal 后 open 的产物 audit strictCompatible=true + fromJsonStrict 成功；
3. **nonce 密文不同、内容指纹稳定**：同内容两次 seal 密文不同（envelope nonce），open 后规范化指纹相同（911 语义跨密文稳定）；
4. **既有 seal/open 零回归**（isSealed 识别、标记头错误 DATA_CORRUPTION）。

## 兼容性

纯测试编排轮；零生产代码变更（实证出缺陷按先例修复并补记本 spec）。
