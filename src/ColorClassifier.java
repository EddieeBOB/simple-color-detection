import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/** Classifies whether an object in a region of interest is "good" (red). */
public class ColorClassifier {

    private final double matchThreshold;

    public ColorClassifier() {
        this(0.30);
    }

    public ColorClassifier(double matchThreshold) {
        this.matchThreshold = matchThreshold;
    }

    /** A centered box covering 30%..70% of each dimension. */
    public static Rectangle centerRoi(int w, int h) {
        return new Rectangle(w * 3 / 10, h * 3 / 10, w * 2 / 5, h * 2 / 5);
    }

    /** True if (r,g,b) is within the accept (red) HSV range. */
    boolean isAcceptColor(int r, int g, int b) {
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        float hue = hsb[0] * 360f;
        float sat = hsb[1] * 100f;
        float bri = hsb[2] * 100f;
        boolean hueIsRed = (hue >= 345f || hue <= 15f);
        return hueIsRed && sat >= 40f && bri >= 30f;
    }

    /** Count in-range pixels in the ROI; ACCEPT if the fraction meets the threshold. */
    public SortDecision classify(BufferedImage frame, Rectangle roi) {
        
        //Set (x,y) relative to the Rectange @ center
        int x0 = Math.max(0, roi.x);
        int y0 = Math.max(0, roi.y);
        int x1 = Math.min(frame.getWidth(), roi.x + roi.width);
        int y1 = Math.min(frame.getHeight(), roi.y + roi.height);

        //Simple n^2 for loop
        long total = 0, match = 0;
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                int rgb = frame.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                total++;
                if (isAcceptColor(r, g, b)) match++;
            }
        }
        if (total == 0) return SortDecision.unknown();
        double ratio = (double) match / total;
        Verdict v = ratio >= matchThreshold ? Verdict.ACCEPT : Verdict.REJECT;
        return new SortDecision(v, ratio);
    }
}
