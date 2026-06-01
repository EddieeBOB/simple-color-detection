# ChromaFuse

Real-time color detection system that fuses vision-computed cursor corrections with hardware mouse input at the USB HID level — bypassing OS-layer event injection entirely.

## Why I built this

I wanted to explore hardware/software co-design at the input layer: can a microcontroller intercept raw USB mouse data, augment it with computer vision output, and re-emit a single clean HID stream — all before the OS processes anything? The project became a full sensor-to-actuator pipeline spanning Java computer vision, serial communication, and embedded USB HID on an AVR microcontroller.

## Tech Stack

| Technology | Purpose | Why this choice |
|---|---|---|
| Java | Screen capture & color detection | AWT `Robot` API gives direct screen access without native bindings |
| Arduino Leonardo | Hardware mouse emulation | Only common AVR board with native USB HID support built-in |
| USB Host Shield | Reading physical mouse input | Intercepts raw USB packets before the OS — enables true hardware-level fusion |
| HSV Color Space | Object identification | Isolates hue from brightness, making thresholds robust to lighting changes that break RGB detection |
| JSSC | Java–Arduino serial link | Lightweight serial library with low-latency blocking I/O |

## Key Features

- **Scans** screen regions at ~50 FPS using parallel pixel stream processing
- **Identifies** objects via HSV color range filtering, decoupled from ambient lighting
- **Fuses** live mouse input with vision-computed correction vectors before the OS sees any movement
- **Renders** a transparent overlay showing the detection radius and matched pixels in real time
- **Transmits** compact 3-byte delta packets over serial to minimize Java-to-Arduino latency
- **Exposes** tunable deadzone, sensitivity, and color thresholds without recompiling

## Screenshots / Demo

> Add a GIF or screenshot here showing the overlay in action.
> Suggested: `assets/demo.gif` — record with OBS or ShareX, crop to the overlay window.

## Getting Started

**Prerequisites:** Java JDK 18+, Arduino IDE, USB Host Shield library, Arduino Leonardo

### Arduino
```bash
# 1. Install USB Host Shield library via Arduino IDE: Sketch > Include Library > Manage Libraries
# 2. Open ChromaFuse.ino, select Board: Arduino Leonardo, select correct COM port
# 3. Upload the sketch
```

### Java
```bash
# Place jssc-2.10.2.jar and slf4j-simple-1.7.36.jar in lib/
javac -cp "lib/*" src/HSVdetect.java
# Update COM port constant in HSVdetect.java (~line 25), then:
java -cp "lib/*:bin" HSVdetect
```

### Tuning
Edit `isTargetColor()` for HSV ranges, `MOUSE_SCALE_X/Y` for sensitivity, and `DEADZONE` for movement filtering.

## Technical Highlights

- **Hardware-level input fusion** — the Arduino reads raw USB HID packets from the physical mouse via the Host Shield, adds vision-computed deltas, and re-emits a single merged HID stream. The host OS only ever sees one input device.
- **Parallel pixel scanning** — the screen region is processed with Java parallel streams that average all matching pixels to compute an object centroid. This keeps per-frame latency well under 20 ms at 50 FPS.
- **HSV over RGB** — RGB thresholds break under different lighting or monitor gamma. Chose HSV so the hue channel can be bounded tightly while value/saturation ranges absorb lighting variation.
- **Minimal serial protocol** — designed a 3-byte signed delta packet (`dx`, `dy`, checksum) to keep round-trip serial latency below one frame; profiling showed serial was the dominant bottleneck, not pixel processing.

## What I Learned

Designing across two languages and two processors forced me to think in terms of latency budgets — serial round-trip dominated the pipeline, not the computer vision work. I deepened my understanding of USB HID at the protocol level and why hardware/software boundaries matter in systems design. Most concretely: choosing the right color space (HSV) turned a brittle detector into a reliable one, which was a direct lesson in how algorithmic choices outweigh parameter tuning.

## Roadmap

- [ ] Replace color detection with a lightweight ONNX model for shape-aware, color-agnostic object recognition
- [ ] Load color profiles and sensitivity from a config file instead of hardcoded constants
- [ ] Auto-detect Arduino COM port on startup to eliminate manual configuration
- [ ] Port screen capture to a JNI native layer for sub-millisecond capture latency
- [ ] Add an interactive calibration mode with live visual feedback for threshold tuning
