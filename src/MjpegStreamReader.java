import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** No clue how this works */

/**
 * Reads an MJPEG (multipart/x-mixed-replace) HTTP stream on a single persistent
 * connection, decoding each JPEG part into a BufferedImage. Reconnects on failure.
 */
public class MjpegStreamReader {

    private final String streamUrl;
    private final Consumer<BufferedImage> onFrame;

    private volatile boolean running = false;
    private volatile HttpURLConnection connection;
    private Thread thread;

    public MjpegStreamReader(String streamUrl, Consumer<BufferedImage> onFrame) {
        this.streamUrl = streamUrl;
        this.onFrame = onFrame;
    }

    public void start() {
        running = true;
        thread = new Thread(this::runLoop, "mjpeg-reader");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
        HttpURLConnection c = connection;
        if (c != null) c.disconnect();   // unblocks a pending read
        if (thread != null) thread.interrupt();
    }

    private void runLoop() {
        while (running) {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(streamUrl).openConnection();
                c.setConnectTimeout(5000);
                c.setReadTimeout(8000);
                connection = c;
                try (InputStream in = c.getInputStream()) {
                    parseStream(in, onFrame);   // blocks until stream ends or errors
                } finally {
                    c.disconnect();
                }
            } catch (Exception e) {
                if (running) {
                    System.err.println("Stream error: " + e.getMessage() + " — reconnecting");
                }
            }
            if (running) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    /**
     * Parse a multipart/x-mixed-replace body: for each part, read its
     * Content-Length, skip the rest of the part headers, read exactly that many
     * bytes as a JPEG, decode, and deliver it. Boundary/Content-Type lines are
     * ignored. Returns when the stream ends.
     */
    public static void parseStream(InputStream in, Consumer<BufferedImage> onFrame)
            throws IOException {
        while (true) {
            String line = readLine(in);
            if (line == null) return;                    // EOF
            if (line.toLowerCase().startsWith("content-length:")) {
                int len = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
                // consume the rest of the part headers up to the blank line
                String h;
                while ((h = readLine(in)) != null && !h.isEmpty()) { /* skip */ }
                byte[] jpeg = readFully(in, len);
                try {
                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(jpeg));
                    if (img != null) onFrame.accept(img);
                } catch (IOException decodeError) {
                    // corrupt frame: skip it, stay aligned to the next boundary (Content-Length already consumed)
                }
            }
        }
    }

    /** Read one CRLF- or LF-terminated ASCII line. Returns null at EOF. */
    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        boolean sawAny = false;
        int b;
        while ((b = in.read()) != -1) {
            sawAny = true;
            if (b == '\n') break;
            if (b != '\r') buf.write(b);
        }
        if (!sawAny && buf.size() == 0) return null;
        return buf.toString("US-ASCII");
    }

    /** Read exactly n bytes or throw. */
    private static byte[] readFully(InputStream in, int n) throws IOException {
        byte[] data = new byte[n];
        int off = 0;
        while (off < n) {
            int r = in.read(data, off, n - off);
            if (r == -1) throw new IOException("stream ended mid-frame");
            off += r;
        }
        return data;
    }
}
