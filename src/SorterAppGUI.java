import java.awt.BorderLayout;
import java.awt.Rectangle;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/** Main window: live feed with the color-classification verdict, plus Start/Stop. */
public class SorterAppGUI extends JFrame {

    private static final String STREAM_URL = "http://192.168.1.212/stream";

    private final ColorClassifier classifier = new ColorClassifier();
    private final VideoPanel videoPanel = new VideoPanel();

    private volatile boolean streaming = false;
    private MjpegStreamReader reader;

    private final JButton startButton = new JButton("Start");
    private final JButton stopButton = new JButton("Stop");

    public SorterAppGUI() {
        super("Color Sorter");
        buildUI();
        setVisible(true);
    }

    private void buildUI() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1280, 720);

        stopButton.setEnabled(false);

        JPanel controls = new JPanel();
        controls.add(startButton);
        controls.add(stopButton);

        startButton.addActionListener(e -> startStream());
        stopButton.addActionListener(e -> stopStream());

        setLayout(new BorderLayout());
        add(videoPanel, BorderLayout.CENTER);
        add(controls, BorderLayout.SOUTH);
    }

    private void startStream() {
        reader = new MjpegStreamReader(STREAM_URL, this::onFrame);
        reader.start();
        streaming = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
    }

    private void stopStream() {
        streaming = false;
        if (reader != null) reader.stop();
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        SwingUtilities.invokeLater(() -> videoPanel.update(null, SortDecision.unknown(), null));
    }

    /** Runs on the reader thread: classify, then marshal the UI update to the EDT. */
    private void onFrame(java.awt.image.BufferedImage frame) {
        if (!streaming) return;
        Rectangle roi = ColorClassifier.centerRoi(frame.getWidth(), frame.getHeight());
        SortDecision decision = classifier.classify(frame, roi);
        SwingUtilities.invokeLater(() -> {
            if (!streaming) return;
            videoPanel.update(frame, decision, roi);
        });
    }
}
