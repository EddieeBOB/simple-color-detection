import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;
import jssc.SerialPort;
import jssc.SerialPortException;

public class HSVdetect {
    private static volatile int targetX = 0;
    private static volatile int targetY = 0;
    private static volatile int centerX;
    private static volatile int centerY;
    private static final int radius = 30;
    private static Rectangle screenSize;
    private static volatile BufferedImage screenCapture;
    private static Robot robot;
    private static SerialPort serialPort;
    private static final List<Point> targetPixels = new CopyOnWriteArrayList<>();
    private static int[] pixels;

    public static void main(String[] args) throws AWTException {
        // Initialize robot and screen capture
        robot = new Robot();
        screenSize = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        centerX = screenSize.width / 2;
        centerY = screenSize.height / 2;
        
        // Initialize serial port
        try {
            serialPort = new SerialPort("COM5"); // Change to your Arduino port
            serialPort.openPort();
            serialPort.setParams(SerialPort.BAUDRATE_115200, 
                               SerialPort.DATABITS_8,
                               SerialPort.STOPBITS_1,
                               SerialPort.PARITY_NONE);
        } catch (SerialPortException e) {
            System.err.println("Failed to initialize serial port: " + e.getMessage());
        }

        // Add shutdown hook to close serial port
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (serialPort != null && serialPort.isOpened()) {
                try {
                    serialPort.closePort();
                } catch (SerialPortException e) {
                    System.err.println("Error closing serial port: " + e.getMessage());
                }
            }
        }));

        // Start serial reader thread to print Arduino logs
        new Thread(() -> {
            try {
                while (serialPort != null && serialPort.isOpened()) {
                    if (serialPort.getInputBufferBytesCount() > 0) {
                        String line = serialPort.readString();
                        if (line != null && !line.trim().isEmpty()) {
                            System.out.println("[Arduino] " + line.trim());
                        }
                    }
                    Thread.sleep(10); // Small delay to avoid busy waiting
                }
            } catch (Exception e) {
                System.err.println("Error reading from serial port: " + e.getMessage());
            }
        }).start();

        // Create transparent overlay
        JFrame overlay = new JFrame("Aimbot Overlay");
        overlay.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        overlay.setUndecorated(true);
        overlay.setBackground(new Color(0, 0, 0, 0)); // Transparent
        overlay.setAlwaysOnTop(true);
        overlay.setBounds(screenSize);
        overlay.setLayout(new BorderLayout());
        
        // Without this, the window is draggable from any non-transparent point
        overlay.getRootPane().putClientProperty("apple.awt.draggableWindowBackground", false);

        // Custom drawing panel
        JPanel canvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                
                // Draw search area
                g2d.setColor(Color.GREEN);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
                
                // Draw target if found
                if (!targetPixels.isEmpty()) {
                    g2d.setColor(Color.RED);
                    g2d.fillOval(targetX - 5, targetY - 5, 10, 10);
                    
                    // Visualize detected pixels (optional)
                    g2d.setColor(new Color(255, 0, 0, 100));
                    for (Point p : targetPixels) {
                        g2d.fillRect(p.x, p.y, 1, 1);
                    }
                }
            }
        };
        canvas.setOpaque(false);
        overlay.add(canvas, BorderLayout.CENTER);
        overlay.setVisible(true);

        // Detection thread
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                long startTime = System.nanoTime();
                
                // Capture screen region around center for better performance
                int captureMargin = 10;
                int captureX = Math.max(0, centerX - radius - captureMargin);
                int captureY = Math.max(0, centerY - radius - captureMargin);
                int captureW = Math.min(screenSize.width - captureX, radius * 2 + captureMargin * 2);
                int captureH = Math.min(screenSize.height - captureY, radius * 2 + captureMargin * 2);
                Rectangle captureRect = new Rectangle(captureX, captureY, captureW, captureH);
                screenCapture = robot.createScreenCapture(captureRect);
                
                // Local center coordinates within the captured image
                int localCenterX = centerX - captureX;
                int localCenterY = centerY - captureY;
                
                // Find targets
                findTargets(localCenterX, localCenterY, captureX, captureY);
                
                // Send to Arduino
                if (!targetPixels.isEmpty()) {
                    sendToArduino();
                }

                // Update overlay
                SwingUtilities.invokeLater(overlay::repaint);
                
                // Maintain ~144 FPS
                long elapsed = System.nanoTime() - startTime;
                long frameTimeNs = 1_000_000_000 / 144;  // ~6.94 ms per frame

                long sleepNs = frameTimeNs - elapsed;
                if (sleepNs > 0) {
                    try {
                        Thread.sleep(sleepNs / 1_000_000, (int)(sleepNs % 1_000_000));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                //System.out.println("FPS: " + (1_000_000_000.0 / (System.nanoTime() - startTime)));
            }
        }).start();
    }

    private static void findTargets(int localCenterX, int localCenterY, int captureX, int captureY) {
        targetPixels.clear();
        if (screenCapture == null) return;
        
        int width = screenCapture.getWidth();
        int height = screenCapture.getHeight();
        if (pixels == null || pixels.length < width * height) {
            pixels = new int[width * height];
        }
        screenCapture.getRGB(0, 0, width, height, pixels, 0, width);

        int startY = Math.max(0, localCenterY - radius);
        int endY = Math.min(height, localCenterY + radius);

        // Parallel scan across rows
        IntStream.range(startY, endY).parallel().forEach(y -> {
            for (int x = Math.max(0, localCenterX - radius); x < Math.min(width, localCenterX + radius); x++) {
                int dx = x - localCenterX;
                int dy = y - localCenterY;
                if (dx * dx + dy * dy <= radius * radius) {
                    int rgb = pixels[y * width + x];
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    if (isTargetColor(r, g, b)) {
                        targetPixels.add(new Point(captureX + x, captureY + y));
                    }
                }
            }
        });

        // Average position
        if (!targetPixels.isEmpty()) {
            long sumX = 0, sumY = 0;
            for (Point p : targetPixels) {
                sumX += p.x;
                sumY += p.y;
            }
            targetX = (int) (sumX / targetPixels.size());
            targetY = (int) (sumY / targetPixels.size());
        }
    }

    private static boolean isTargetColor(int r, int g, int b) {
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        float hue = hsb[0] * 360;
        float saturation = hsb[1] * 100;
        float brightness = hsb[2] * 100;

        return (hue >= 281 && hue <= 300) &&
               (saturation >= 43 && saturation <= 76) &&
               (brightness >= 59 && brightness <= 100);
    }

    private static void sendToArduino() {
        if (serialPort != null && serialPort.isOpened()) {
            int deltaX = targetX - centerX;
            int deltaY = targetY - centerY;
            
            // Only send if target is significant enough
            if (Math.abs(deltaX) > 15 || Math.abs(deltaY) > 15) {
                String data = deltaX + "," + deltaY + "\n";
                try {
                    serialPort.writeBytes(data.getBytes());
                } catch (SerialPortException e) {
                    System.err.println("Arduino communication error: " + e.getMessage());
                }
            }
        }
    }
}