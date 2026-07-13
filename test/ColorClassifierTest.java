import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Rectangle;

import org.junit.jupiter.api.Test;

class ColorClassifierTest {

    private final ColorClassifier classifier = new ColorClassifier(); // threshold 0.30
    private final Color red = new Color(220, 30, 30);
    private final Color green = new Color(40, 180, 40);
    // Whole-image ROI so matchRatio == redFraction.
    private final Rectangle full = new Rectangle(0, 0, 100, 100);

    @Test
    void allRedIsAccepted() {
        SortDecision d = classifier.classify(TestSupport.solid(100, 100, red), full);
        assertEquals(Verdict.ACCEPT, d.verdict());
        assertTrue(d.matchRatio() > 0.99, "ratio ~1.0");
    }

    @Test
    void allGreenIsRejected() {
        SortDecision d = classifier.classify(TestSupport.solid(100, 100, green), full);
        assertEquals(Verdict.REJECT, d.verdict());
        assertTrue(d.matchRatio() < 0.01, "ratio ~0.0");
    }

    @Test
    void fortyPercentRedIsAccepted() {
        SortDecision d = classifier.classify(
            TestSupport.redFractionImage(100, 100, 0.40, red, green), full);
        assertEquals(Verdict.ACCEPT, d.verdict());
    }

    @Test
    void twentyPercentRedIsRejected() {
        SortDecision d = classifier.classify(
            TestSupport.redFractionImage(100, 100, 0.20, red, green), full);
        assertEquals(Verdict.REJECT, d.verdict());
    }

    @Test
    void redHueWraparoundIsRecognized() {
        assertTrue(classifier.isAcceptColor(255, 0, 0), "hue ~0 is red");
        assertTrue(classifier.isAcceptColor(255, 0, 50), "hue ~348 is red (wraparound)");
        assertFalse(classifier.isAcceptColor(40, 180, 40), "green is not red");
    }

    @Test
    void centerRoiGeometry() {
        assertEquals(new Rectangle(30, 30, 40, 40), ColorClassifier.centerRoi(100, 100));
    }
}
