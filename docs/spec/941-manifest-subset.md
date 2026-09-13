# 941 — ExportManifest 子集校验

> 来源：I 会话第 41 轮 = effort #941（impl 690 续）。rsync --partial 思想——增量搬运场景只核对提供的子集。

## 目标

- `ExportManifest.verifySubset(manifestJson, contents)`：只核对提供的会话子集（规范化口径与 verifyCanonical 配对）——manifest 中未提供的条目不计缺失/多出；contents 空/null fail-fast（子集校验至少验一项）；子集含 manifest 没有的会话 = mismatch（异常显形）。
- 篡改仍检出（规范化不放过真篡改）。

## 兼容性

纯增量：公共类新增静态方法；既有 verify/verifyCanonical 零变化。
