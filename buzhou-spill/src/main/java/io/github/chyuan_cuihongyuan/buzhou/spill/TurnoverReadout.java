package io.github.chyuan_cuihongyuan.buzhou.spill;

/**
 * 库存周转读面（spec 1836 / T2873 / impl 1437）——供应链库存周转
 *（inventory turnover）思想：库存效率两读数——**周转次数**（单位周期内
 * 库存转几轮 = 消费量/库存量，越高库存越「活」）与**耗尽视界**（当前
 * 消费速率下库存多久见底 = 库存量/速率，越短越该补货/清理）。映射到
 * spill：库存=handle 存量，消费=回读/逐出流量——周转贴地=死库存（TTL
 * 该激进），视界短=热门快耗（该预热）。
 *
 * <p>纯函数零状态、只读不裁决（补货/清理策略归宿主）。
 */
public final class TurnoverReadout {

    private TurnoverReadout() {
    }

    /**
     * 周转次数 = consumedPerPeriod / stockUnits（无库存 -1 哨兵——库存为零
     * 无周转语义；零消费返回 0——死库存）。
     */
    public static double turns(long stockUnits, long consumedPerPeriod) {
        if (stockUnits < 0 || consumedPerPeriod < 0) {
            throw new IllegalArgumentException(String.format(
                    "入参不能为负：stock=%d, consumed=%d", stockUnits, consumedPerPeriod));
        }
        if (stockUnits == 0) {
            return -1d;
        }
        return (double) consumedPerPeriod / stockUnits;
    }

    /**
     * 耗尽视界（毫秒）= stockUnits / consumedPerMillis（向上取整——保守）；
     * 零速率 -1 哨兵（无消费永不耗尽）；零库存 0（已空）。
     */
    public static long depletionHorizonMillis(long stockUnits, double consumedPerMillis) {
        if (stockUnits < 0) {
            throw new IllegalArgumentException("stockUnits 不能为负：" + stockUnits);
        }
        if (Double.isNaN(consumedPerMillis) || consumedPerMillis < 0) {
            throw new IllegalArgumentException(
                    "consumedPerMillis 须 ≥ 0 非 NaN：" + consumedPerMillis);
        }
        if (stockUnits == 0) {
            return 0L;
        }
        if (consumedPerMillis == 0) {
            return -1L;
        }
        return (long) Math.ceil(stockUnits / consumedPerMillis);
    }
}
