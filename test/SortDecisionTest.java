import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SortDecisionTest {

    @Test
    void storesVerdictAndRatio() {
        SortDecision d = new SortDecision(Verdict.ACCEPT, 0.42);
        assertEquals(Verdict.ACCEPT, d.verdict());
        assertEquals(0.42, d.matchRatio());
    }

    @Test
    void unknownIsUnknownWithZeroRatio() {
        SortDecision u = SortDecision.unknown();
        assertEquals(Verdict.UNKNOWN, u.verdict());
        assertEquals(0.0, u.matchRatio());
    }
}
