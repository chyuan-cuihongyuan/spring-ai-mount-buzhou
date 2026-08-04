package io.github.chyuan_cuihongyuan.buzhou.core.spi;

/**
 * 事实模型（spec 07 Hook→state→Attachment 闭环）。
 *
 * <p>Hook 确定性采集的会话级事实记录，建在 SessionStateStore 通用 KV 上（不建专项表）。
 * key 命名空间约定：{@code fact.{producer}.{name}}。
 *
 * @param key         事实 key（含命名空间前缀，如 {@code fact.risk-analysis.table-123}）
 * @param value       事实载荷（任意对象，由采集器定义；FactStore 序列化为 JSON 存入 StateEntry.value）
 * @param producer    来源 Hook/采集器名（入 key 命名空间）
 * @param createdTurn 采集轮次
 * @param ttl         存活轮次：剩余轮次内累积注入、过期自动停注；1=一次性消费，大值=持久累积
 */
public record Fact(String key, Object value, String producer, int createdTurn, int ttl) {

    public Fact {
        key = key == null ? "" : key;
        producer = producer == null ? "" : producer;
        ttl = Math.max(1, ttl);
    }

    /** 构造事实 key（命名空间约定 fact.{producer}.{name}）。 */
    public static String keyFor(String producer, String name) {
        return "fact." + producer + "." + name;
    }
}
