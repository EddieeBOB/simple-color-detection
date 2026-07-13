import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import java.awt.Rectangle;

import org.junit.jupiter.api.Test;

class VideoPanelTest {

    @Test
    void verdictColorMapping() {
        assertEquals(Color.GREEN, VideoPanel.verdictColor(Verdict.ACCEPT));
        assertEquals(Color.RED, VideoPanel.verdictColor(Verdict.REJECT));
        assertEquals(Color.LIGHT_GRAY, VideoPanel.verdictColor(Verdict.UNKNOWN));
    }

    @Test
    void updateAcceptsNullAndRealFrame() {
        VideoPanel p = new VideoPanel();
        assertDoesNotThrow(() -> {
            p.update(null, SortDecision.unknown(), null);          // disconnected state
            p.update(TestSupport.solid(20, 20, Color.RED),
                     new SortDecision(Verdict.ACCEPT, 1.0),
                     new Rectangle(5, 5, 10, 10));
        });
    }
}
