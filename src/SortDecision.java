/** Immutable classification result (Java 11 compatible — not a record). */
public final class SortDecision {
    private final Verdict verdict;
    private final double matchRatio;

    public SortDecision(Verdict verdict, double matchRatio) {
        this.verdict = verdict;
        this.matchRatio = matchRatio;
    }

    public static SortDecision unknown() {
        return new SortDecision(Verdict.UNKNOWN, 0.0);
    }

    public Verdict verdict() {
        return verdict;
    }

    public double matchRatio() {
        return matchRatio;
    }

    @Override
    public String toString() {
        return "SortDecision[verdict=" + verdict + ", matchRatio=" + matchRatio + "]";
    }
}
