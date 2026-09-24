package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Distributed Snapshot 一致快照（spec 5037 / T6175 / impl 2188）——
 * Chandy-Lamport 标记快照思想（Flink barrier/Akka 同源）：
 * 发起者记自身状态并向全部出边发标记；进程首次收标记即记
 * 自身状态、该入边记空、并向全部出边续发标记；同一入边的
 * **第二个**标记到达即封口该信道——信道记录=发起者快照后
 * 发出、接收者在收到标记前收到的报文（在途消息不丢失不重复
 * 归属——全局状态=各进程状态+各信道在途，切成一片一致
 * 历史）。确定性无时间依赖（事件由调用方确定性驱动）。
 *
 * <p>与 SessionArchiver（归档导出）同族不同面：本地全量
 * 导出 vs 分布式一致瞬间。
 */
public final class DistributedSnapshot {

    /** 一次快照的记录结果（进程状态 + 信道在途）。 */
    public record SnapshotResult(Map<Integer, Integer> processStates,
                                 Map<String, List<String>> channelStates) {
    }

    private static final String CHANNEL_ARROW = "->";

    private static final int INITIATOR = 0;

    private static final class Channel {
        final int from;
        final int to;
        final Deque<String> queue = new ArrayDeque<>();
        Deque<String> recorded;
        boolean markerSent;
        boolean closed;

        Channel(int from, int to) {
            this.from = from;
            this.to = to;
        }

        String key() {
            return from + CHANNEL_ARROW + to;
        }
    }

    private final int processCount;
    private final Map<Integer, Integer> liveState = new HashMap<>();
    private final Map<Integer, Integer> recordedState = new HashMap<>();
    private final List<Channel> channels = new ArrayList<>();
    private final Map<String, Channel> byKey = new LinkedHashMap<>();
    private boolean started;
    private boolean recordedAll;

    /** 定构（进程数≥1；0 号为唯一发起者）。 */
    public DistributedSnapshot(int processCount) {
        if (processCount < 1) {
            throw new IllegalArgumentException("processCount≥1：" + processCount);
        }
        this.processCount = processCount;
        for (int pid = 0; pid < processCount; pid++) {
            liveState.put(pid, 0);
        }
    }

    /** 建双向信道（重复边/自环/越界 fail-fast）。 */
    public void addChannel(int a, int b) {
        requireProcess(a);
        requireProcess(b);
        if (a == b) {
            throw new IllegalArgumentException("自环信道：" + a);
        }
        addDirected(a, b);
        addDirected(b, a);
    }

    /** 进程本地状态写入（快照记录的是「记录时刻」的值）。 */
    public void setState(int pid, int state) {
        requireProcess(pid);
        liveState.put(pid, state);
    }

    /** 进程发送应用报文（快照后发出且对端未收标记→计入信道记录）。 */
    public void sendMessage(int from, int to, String payload) {
        Channel channel = requireChannel(from, to);
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("payload 非空");
        }
        channel.queue.addLast(payload);
        if (recordedState.containsKey(from) && !channel.closed) {
            if (channel.recorded == null) {
                channel.recorded = new ArrayDeque<>();
            }
            channel.recorded.addLast(payload);
        }
    }

    /** 进程接收并消费应用报文（FIFO 队头；空队列 fail-fast）。 */
    public String deliver(int from, int to) {
        Channel channel = requireChannel(from, to);
        String payload = channel.queue.pollFirst();
        if (payload == null) {
            throw new IllegalStateException("信道空无可收：" + channel.key());
        }
        return payload;
    }

    /** 发起快照（0 号进程记录自身状态并向全部出边发标记）。 */
    public void startSnapshot() {
        if (started) {
            throw new IllegalStateException("快照已发起（本件单快照语义）");
        }
        started = true;
        recordedState.put(INITIATOR, liveState.get(INITIATOR));
        for (Channel channel : channels) {
            if (channel.from == INITIATOR) {
                channel.markerSent = true;
            }
        }
        checkComplete();
    }

    /**
     * 进程收标记：首次→记自身状态+该入边记空+出边续发标记；
     * 同入边第二次→封口该信道记录。
     */
    public void receiveMarker(int from, int to) {
        Channel channel = requireChannel(from, to);
        if (!started) {
            throw new IllegalStateException("快照未发起不可收标记");
        }
        if (channel.closed) {
            throw new IllegalStateException("信道已封口重复收标记：" + channel.key());
        }
        channel.closed = true;
        if (channel.recorded == null) {
            channel.recorded = new ArrayDeque<>();
        }
        if (!recordedState.containsKey(to)) {
            recordedState.put(to, liveState.get(to));
            for (Channel outgoing : channels) {
                if (outgoing.from == to) {
                    outgoing.markerSent = true;
                }
            }
        }
        checkComplete();
    }

    /** 快照是否完成（全部进程已记录且全部信道已封口）。 */
    public boolean isComplete() {
        return recordedAll;
    }

    /** 记录结果（未完成 fail-fast——不完整快照不出口）。 */
    public SnapshotResult result() {
        if (!recordedAll) {
            throw new IllegalStateException("快照未完成不可出结果");
        }
        Map<Integer, Integer> states = new LinkedHashMap<>(recordedState);
        Map<String, List<String>> channelStates = new LinkedHashMap<>();
        for (Channel channel : channels) {
            channelStates.put(channel.key(),
                    channel.recorded == null ? List.of() : List.copyOf(channel.recorded));
        }
        return new SnapshotResult(states, channelStates);
    }

    private void checkComplete() {
        if (!started || recordedAll) {
            return;
        }
        if (recordedState.size() < processCount) {
            return;
        }
        for (Channel channel : channels) {
            if (!channel.closed) {
                return;
            }
        }
        recordedAll = true;
    }

    private void addDirected(int from, int to) {
        Channel channel = new Channel(from, to);
        channels.add(channel);
        byKey.put(channel.key(), channel);
    }

    private Channel requireChannel(int from, int to) {
        requireProcess(from);
        requireProcess(to);
        Channel channel = byKey.get(from + CHANNEL_ARROW + to);
        if (channel == null) {
            throw new IllegalArgumentException("信道未建：" + from + CHANNEL_ARROW + to);
        }
        return channel;
    }

    private void requireProcess(int pid) {
        if (pid < 0 || pid >= processCount) {
            throw new IllegalArgumentException("进程越界 0.." + (processCount - 1) + "：" + pid);
        }
    }
}
