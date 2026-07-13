import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class MjpegParserTest {

    @Test
    void parsesTwoFrames() throws IOException {
        byte[] jpeg1 = toJpeg(TestSupport.solid(8, 8, Color.RED));
        byte[] jpeg2 = toJpeg(TestSupport.solid(16, 16, Color.GREEN));

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        writePart(body, jpeg1);
        writePart(body, jpeg2);

        List<BufferedImage> frames = new ArrayList<>();
        MjpegStreamReader.parseStream(
            new ByteArrayInputStream(body.toByteArray()), frames::add);

        assertEquals(2, frames.size(), "two frames parsed");
        assertEquals(8, frames.get(0).getWidth(), "frame 1 width");
        assertEquals(16, frames.get(1).getWidth(), "frame 2 width");
    }

    @Test
    void skipsCorruptMiddleFrame() throws IOException {
        // A corrupt frame in the middle should be skipped (parseStream must not
        // throw), and the two good frames on either side still delivered.
        byte[] jpeg1 = toJpeg(TestSupport.solid(8, 8, Color.RED));
        byte[] jpeg2 = toJpeg(TestSupport.solid(16, 16, Color.GREEN));
        byte[] truncated = new byte[jpeg1.length / 2];
        System.arraycopy(jpeg1, 0, truncated, 0, truncated.length);

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        writePart(body, jpeg1);
        writePart(body, truncated);
        writePart(body, jpeg2);

        List<BufferedImage> frames = new ArrayList<>();
        MjpegStreamReader.parseStream(
            new ByteArrayInputStream(body.toByteArray()), frames::add);

        assertEquals(2, frames.size(), "corrupt middle frame skipped, two good frames delivered");
        assertEquals(8, frames.get(0).getWidth(), "frame 1 width (corrupt stream)");
        assertEquals(16, frames.get(1).getWidth(), "frame 2 width (corrupt stream)");
    }

    private static byte[] toJpeg(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return baos.toByteArray();
    }

    private static void writePart(ByteArrayOutputStream out, byte[] jpeg) throws IOException {
        String header = "--frame\r\n"
            + "Content-Type: image/jpeg\r\n"
            + "Content-Length: " + jpeg.length + "\r\n\r\n";
        out.write(header.getBytes(StandardCharsets.US_ASCII));
        out.write(jpeg);
        out.write("\r\n".getBytes(StandardCharsets.US_ASCII));
    }
}
