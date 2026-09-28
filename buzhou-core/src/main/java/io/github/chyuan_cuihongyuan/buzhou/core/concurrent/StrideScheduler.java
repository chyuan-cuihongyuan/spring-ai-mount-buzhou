package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.HashMap;
import java.util.Map;

/**
 * Stride Scheduler 步幅调度（spec 6027 / T6249 续 / impl 2228）——
 * MIT exokernel stride 思想：**确定性比例份额**——客户端按
 * 票数取步幅 stride = BASE/tickets（票多步幅小），每被调度
 * 一次 pass += stride；serve 恒选 pass 最小者（并列按 id 小
 * ——完全确定性，无随机）——WRR 短窗倾斜与彩票随机的病解。
 * 长程 served 次数精确趋近 tickets 比例。全客户端扫描选最小
 * （调度面客户端数小——O(n) 简洁换正确）。
 *
 * <p>与 WeightedRoundRobin（spec 5026）同族不同面：平滑插值
 * 轮询 vs 单调 pass 记账；与 DeficitRoundRobin（spec 5027）
 * 不同面：字节亏空 vs 票数比例。确定性定构（同注册同序列
 * 同出序）。
 */
public final class StrideScheduler {

    private static final long BASE_STRIDE = 1_000_000L;

    private static final class Client {
        final long tickets;
        long pass;

        Client(long tickets) {
            this.tickets = tickets;
        }
    }

    private final Map<Long, Client> clients = new HashMap<>();

    /** 注册客户端（tickets≤0/重复 fail-fast）。 */
    public void register(long clientId, long tickets) {
        if (tickets <= 0) {
            throw new IllegalArgumentException("票数必须为正: " + tickets);
        }
        if (clients.containsKey(clientId)) {
            throw new IllegalArgumentException("客户端已注册: " + clientId);
        }
        clients.put(clientId, new Client(tickets));
    }

    /** 调度下一客户端（pass 最小、并列 id 小；其 pass += stride）。 */
    public long serve() {
        if (clients.isEmpty()) {
            throw new IllegalArgumentException("无已注册客户端");
        }
        long bestId = Long.MAX_VALUE;
        long bestPass = Long.MAX_VALUE;
        for (Map.Entry<Long, Client> e : clients.entrySet()) {
            long pass = e.getValue().pass;
            if (pass < bestPass || (pass == bestPass && e.getKey() < bestId)) {
                bestId = e.getKey();
                bestPass = pass;
            }
        }
        Client best = clients.get(bestId);
        best.pass += BASE_STRIDE / best.tickets;
        return bestId;
    }

    /** 注销客户端（缺席 fail-fast）。 */
    public void remove(long clientId) {
        if (clients.remove(clientId) == null) {
            throw new IllegalArgumentException("客户端未注册: " + clientId);
        }
    }

    /** 客户端数读数。 */
    public int clientCount() {
        return clients.size();
    }

    /** 客户端当前 pass 读数（缺席 fail-fast）。 */
    public long passOf(long clientId) {
        Client client = clients.get(clientId);
        if (client == null) {
            throw new IllegalArgumentException("客户端未注册: " + clientId);
        }
        return client.pass;
    }

    /** 客户端票数读数（缺席 fail-fast）。 */
    public long ticketsOf(long clientId) {
        Client client = clients.get(clientId);
        if (client == null) {
            throw new IllegalArgumentException("客户端未注册: " + clientId);
        }
        return client.tickets;
    }
}
