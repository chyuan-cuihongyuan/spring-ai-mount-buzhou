# impl 1608 — 确定性散列公共件（spec 2057 / T3215–T3216 / R58）

纵切片：`DeterministicHash`（core/metrics 公共件主）+ 五调用点收敛
（HllCardinalitySketch/FrequencySketch/CuckooFilter/ConsistentHashRing/
SimHashFingerprint 私有内联删除、跨包 import）。

- 同一性：五件既有测试零改动 36/36 全绿。
- 教训入档：收敛脚本的声明替换必须先验证内联已删（半收敛=编译碎）；
  surefire 报告缓存以真实退出码为准。
