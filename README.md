# Physical Color Sorter

A Java Swing application that watches a live ESP32-CAM video feed and classifies
whatever object is sitting in a center region of the frame as **good** (red)
or **bad** (not red), showing the verdict live over the video.

## What it does

1. A background thread opens a single persistent HTTP MJPEG connection to an
   ESP32-CAM and decodes each JPEG frame as it arrives.
2. Every frame is scanned in a fixed center region of interest (ROI), and each
   pixel in that ROI is checked against an "accept color" (red) definition in
   HSV space.
3. If enough of the ROI's pixels match (by default, at least 30%), the frame
   is classified `ACCEPT`; otherwise `REJECT`. If there's no frame yet, the
   state is `UNKNOWN`.
4. The Swing window shows the live feed, draws the ROI box, and overlays the
   current verdict and match percentage.

## Tech stack

- Java 11+ (Swing/AWT for the UI, `javax.imageio` for JPEG decoding,
  `java.net.HttpURLConnection` for the MJPEG connection)
- No third-party **runtime** dependencies, no build tool — plain `javac`/`java`.
  Tests use JUnit 5 via a single console-standalone jar in `lib/` (test-only)
- All classes live in the default package, compiled straight to `bin/`

## How to run

```bash
javac -d bin src/*.java
java -cp bin Main
```

This opens a window titled "Color Sorter" with **Start** and **Stop** buttons.
Click **Start** to connect to the camera stream; once frames arrive, the ROI
box and live verdict appear. Click **Stop** to disconnect. If the camera is
unreachable, the panel shows "Connecting…" and the console logs reconnect
attempts — that's expected, not a crash.

## How to run the tests

The tests use **JUnit 5 (Jupiter)** run through the JUnit Platform Console
Standalone launcher — a single jar, no Maven/Gradle needed. It lives in `lib/`,
which is gitignored, so fetch it once if you don't have it:

```bash
curl -Lo lib/junit-platform-console-standalone-1.14.4.jar \
  https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.14.4/junit-platform-console-standalone-1.14.4.jar
```

Compile the app and the tests, then run them all:

```bash
JUNIT=lib/junit-platform-console-standalone-1.14.4.jar
javac -d bin src/*.java
javac -cp "bin:$JUNIT" -d bin test/*.java
java -jar "$JUNIT" execute --class-path bin --scan-class-path
```

> JUnit **5** (Jupiter 5.14) is used because it runs on Java 11. JUnit **6**
> requires Java 17+; once you're on a newer JDK, bump the jar to the `6.x` line —
> the `@Test` / `Assertions` API is identical.

## ESP32-CAM setup

The camera firmware lives in `esp32cam-rtsp/` (a PlatformIO project). Flash
it to an ESP32-CAM board and it serves an MJPEG stream over HTTP. The app is
currently pointed at:

```
http://192.168.1.212/stream
```

(hardcoded as `STREAM_URL` in `src/SorterAppGUI.java` — update it if your
camera has a different address). The stream is a single-connection MJPEG
feed, so only **one** viewer can be connected at a time — if you have the
stream open in a browser or another tool, disconnect it before clicking
**Start** in the app.

## Tuning the accept color / threshold

Color classification lives in `src/ColorClassifier.java`:

- `ColorClassifier()` uses the default match threshold of `0.30` (30% of ROI
  pixels must match to ACCEPT); construct with `new ColorClassifier(0.5)` etc.
  to change it.
- `isAcceptColor(int r, int g, int b)` defines the accept color itself. Out
  of the box it accepts red: hue in `[0°,15°]` or `[345°,360°]`, saturation
  ≥ 40%, brightness ≥ 30%. Edit the hue/saturation/brightness bounds here to
  target a different color.
- The region of interest is computed by `ColorClassifier.centerRoi(w, h)`, a
  box covering the center 30%–70% of each dimension of the frame. Adjust the
  fractions there if you want a bigger/smaller/offset ROI.
