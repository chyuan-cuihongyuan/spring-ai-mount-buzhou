# 1113 — ConfigMaps indexed 属性数字键归一（M 系 R12）

**What to build:** normalizeValue Map 分支加数字键归一（全数字键 → 数值序 List）。

**Blocked by:** T2271 / T2272（同轮 shape+verify；发现源头 T2267/R9）。

**Status:** done

- [x] 数字键归一（数值序排序防字典序 10<2；混合键保持 Map；嵌套递归）
- [x] ConfigMapsIndexedCoerceTest 三断言（两项归一/跨十位排序/混合不误伤）
- [x] core 配置域既有 43 用例零回归

## Done

验证：定向测试绿。commit 见本轮 fix 提交。
