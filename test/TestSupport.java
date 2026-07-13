import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** Image-builder helpers shared by the JUnit tests. */
public class TestSupport {

    private TestSupport() { }

    /** Solid image filled with one color. */
    public static BufferedImage solid(int w, int h, Color c) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(c);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return img;
    }

    /** Image where the left `redFraction` of columns are `red`, the rest `green`. */
    public static BufferedImage redFractionImage(int w, int h, double redFraction,
                                                 Color red, Color green) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(green);
        g.fillRect(0, 0, w, h);
        int redCols = (int) Math.round(w * redFraction);
        g.setColor(red);
        g.fillRect(0, 0, redCols, h);
        g.dispose();
        return img;
    }
}
