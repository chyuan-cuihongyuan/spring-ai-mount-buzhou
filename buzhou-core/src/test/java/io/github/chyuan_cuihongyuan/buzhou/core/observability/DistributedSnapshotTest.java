package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5037 / T6176：一致快照合同——教科书两进程场景
 * （在途报文归信道不归进程）、后续报文不属记录、完成判定、
 * 畸形 fail-fast。
 */
class DistributedSnapshotTest {

    @Test
    void textbookTwoProcessSnapshotShouldPinInflightToChannel() {
        DistributedSnapshot snapshot = new DistributedSnapshot(2);
        snapshot.addChannel(0, 1);
        snapshot.setState(0, 1);
        snapshot.setState(1, 2);
        snapshot.sendMessage(0, 1, "m1");
        snapshot.startSnapshot();
        snapshot.sendMessage(0, 1, "m2");
        assertThat(snapshot.deliver(0, 1)).isEqualTo("m1");
        snapshot.receiveMarker(0, 1);
        snapshot.receiveMarker(1, 0);
        assertThat(snapshot.isComplete()).isTrue();
        DistributedSnapshot.SnapshotResult result = snapshot.result();
        assertThat(result.processStates()).containsEntry(0, 1).containsEntry(1, 2);
        assertThat(result.channelStates()).containsEntry("0->1", java.util.List.of("m2"));
        assertThat(result.channelStates()).containsEntry("1->0", java.util.List.of());
    }

    @Test
    void messagesAfterMarkerShouldStayOutOfChannelRecord() {
        DistributedSnapshot snapshot = new DistributedSnapshot(2);
        snapshot.addChannel(0, 1);
        snapshot.startSnapshot();
        snapshot.receiveMarker(0, 1);
        snapshot.sendMessage(0, 1, "after-cut");
        snapshot.receiveMarker(1, 0);
        assertThat(snapshot.isComplete()).isTrue();
        assertThat(snapshot.result().channelStates()).containsEntry("0->1", java.util.List.of());
        assertThat(snapshot.deliver(0, 1)).isEqualTo("after-cut");
    }

    @Test
    void incompleteSnapshotShouldRefuseResult() {
        DistributedSnapshot snapshot = new DistributedSnapshot(2);
        snapshot.addChannel(0, 1);
        snapshot.startSnapshot();
        assertThat(snapshot.isComplete()).isFalse();
        assertThatThrownBy(snapshot::result).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void initiatedTwiceShouldFailFast() {
        DistributedSnapshot snapshot = new DistributedSnapshot(2);
        snapshot.addChannel(0, 1);
        snapshot.startSnapshot();
        assertThatThrownBy(snapshot::startSnapshot).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new DistributedSnapshot(0)).isInstanceOf(IllegalArgumentException.class);
        DistributedSnapshot snapshot = new DistributedSnapshot(2);
        assertThatThrownBy(() -> snapshot.addChannel(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> snapshot.addChannel(0, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> snapshot.setState(9, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> snapshot.sendMessage(0, 1, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> snapshot.deliver(0, 1)).isInstanceOf(IllegalArgumentException.class);
        snapshot.addChannel(0, 1);
        assertThatThrownBy(() -> snapshot.receiveMarker(1, 0)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> snapshot.deliver(0, 1)).isInstanceOf(IllegalStateException.class);
        snapshot.startSnapshot();
        snapshot.sendMessage(0, 1, "x");
        snapshot.receiveMarker(0, 1);
        assertThatThrownBy(() -> snapshot.receiveMarker(0, 1)).isInstanceOf(IllegalStateException.class);
    }
}
