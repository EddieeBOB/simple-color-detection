import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

/** Renders the latest camera frame, the ROI box, and the live verdict. */
public class VideoPanel extends JPanel{

    private volatile BufferedImage frame;
    private volatile SortDecision decision = SortDecision.unknown();
    private volatile Rectangle roi;

    public VideoPanel() {
        setBackground(Color.DARK_GRAY);
    }

    /** Called from the EDT (via SwingUtilities.invokeLater) with the newest state. */
    public void update(BufferedImage frame, SortDecision decision, Rectangle roi) {
        this.frame = frame;
        this.decision = (decision == null) ? SortDecision.unknown() : decision;
        this.roi = roi;
        repaint();
    }

    static Color verdictColor(Verdict v) {
        switch (v) {
            case ACCEPT: return Color.GREEN;
            case REJECT: return Color.RED;
            default:     return Color.LIGHT_GRAY;   // UNKNOWN
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        int pw = getWidth(), ph = getHeight();

        BufferedImage f = frame;
        if (f == null) {
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawString("Connecting…", pw / 2 - 40, ph / 2);
            return;
        }

        // Scale to fit, preserving aspect ratio.
        double scale = Math.min((double) pw / f.getWidth(), (double) ph / f.getHeight());
        int dw = (int) (f.getWidth() * scale);
        int dh = (int) (f.getHeight() * scale);
        int ox = (pw - dw) / 2;
        int oy = (ph - dh) / 2;
        g2.drawImage(f, ox, oy, dw, dh, null);

        Rectangle r = roi;
        if (r != null) {
            g2.setColor(Color.YELLOW);
            g2.setStroke(new BasicStroke(2));
            g2.drawRect(ox + (int) (r.x * scale), oy + (int) (r.y * scale),
                        (int) (r.width * scale), (int) (r.height * scale));
        }

        SortDecision d = decision;
        g2.setColor(verdictColor(d.verdict()));
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 22f));
        g2.drawString(d.verdict() + String.format("  (%.0f%% red)", d.matchRatio() * 100), ox + 10, oy + 28);
    }
}
