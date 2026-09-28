package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Lottery Scheduler 彩票调度（spec 6044 / T6287 / impl 2244）——
 * Waldspurger 彩票调度思想：**按票数比例的加权随机选择**——
 * 客户端持 n 张票，每次抽取以 n/总票数概率获胜——WRR 短窗
 * 倾斜（任意前缀比例不成立）与固定轮询（无权重语义）的病解；
 * 长程期望比例精确∝票数，短窗随机（可并发友好——无需全局
 * 顺序）。种子化 Random（同种子同序列——确定性可回放）。
 *
 * <p>与 StrideScheduler（spec 6027）同族不同面：确定性步幅
 * 记账 vs 随机彩票（期望同比例）；与 WeightedReservoirSampler
 * 不同面：流采样 vs 调度选择。
 */
public final class LotteryScheduler {

    private final Map<Long, Long> ticketsOf = new HashMap<>();
    private final List<Long> clientIds = new ArrayList<>();
    private final Random rng;
    private long totalTickets;

    /** 种子注入（同种子同序列——确定性可回放）。 */
    public LotteryScheduler(long seed) {
        this.rng = new Random(seed);
    }

    /** 注册客户端（tickets≤0/重复 fail-fast）。 */
    public void register(long clientId, long tickets) {
        if (tickets <= 0) {
            throw new IllegalArgumentException("票数必须为正: " + tickets);
        }
        if (ticketsOf.containsKey(clientId)) {
            throw new IllegalArgumentException("客户端已注册: " + clientId);
        }
        ticketsOf.put(clientId, tickets);
        clientIds.add(clientId);
        totalTickets += tickets;
    }

    /** 抽取获胜客户端（票数比例；无客户端 fail-fast）。 */
    public long draw() {
        if (clientIds.isEmpty()) {
            throw new IllegalArgumentException("无已注册客户端");
        }
        long winningTicket = (long) (rng.nextDouble() * totalTickets) + 1;
        long cumulative = 0;
        for (long clientId : clientIds) {
            cumulative += ticketsOf.get(clientId);
            if (winningTicket <= cumulative) {
                return clientId;
            }
        }
        return clientIds.get(clientIds.size() - 1);
    }

    /** 注销客户端（缺席 fail-fast）。 */
    public void remove(long clientId) {
        Long tickets = ticketsOf.remove(clientId);
        if (tickets == null) {
            throw new IllegalArgumentException("客户端未注册: " + clientId);
        }
        totalTickets -= tickets;
        clientIds.remove(clientId);
    }

    /** 客户端数读数。 */
    public int clientCount() {
        return clientIds.size();
    }

    /** 总票数读数。 */
    public long totalTickets() {
        return totalTickets;
    }

    /** 客户端票数读数（缺席 fail-fast）。 */
    public long ticketsOf(long clientId) {
        Long tickets = ticketsOf.get(clientId);
        if (tickets == null) {
            throw new IllegalArgumentException("客户端未注册: " + clientId);
        }
        return tickets;
    }
}
