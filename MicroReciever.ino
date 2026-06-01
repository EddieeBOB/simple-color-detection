#include <hidboot.h>
#include <usbhub.h>
#include <Mouse.h>

// Configuration
#define MOUSE_SCALE_X 1.0  // Sensitivity adjustment
#define MOUSE_SCALE_Y 1.0
#define DEADZONE 0.1       // Ignore small movements
#define BAUD_RATE 115200

// USB Host Shield setup
USB Usb;
USBHub Hub(&Usb);
HIDBoot<USB_HID_PROTOCOL_MOUSE> HidMouse(&Usb);

// Global aimbot corrections
volatile int aimX = 0;
volatile int aimY = 0;
volatile bool aimbotActive = false;

class MouseRptParser : public MouseReportParser {
protected:
    void OnMouseMove(MOUSEINFO *mi) {
        int humanX = mi->dX;
        int humanY = mi->dY;
        
        Serial.print("Human move: ");
        Serial.print(humanX);
        Serial.print(", ");
        Serial.println(humanY);
        
        // Add aimbot corrections if active
        if (aimbotActive) {
            humanX += aimX;
            humanY += aimY;
            Serial.print("Aimbot active, corrected to: ");
            Serial.print(humanX);
            Serial.print(", ");
            Serial.println(humanY);
        }
        
        // Apply scaling
        humanX = (int)(humanX * MOUSE_SCALE_X);
        humanY = (int)(humanY * MOUSE_SCALE_Y);
        
        // Apply deadzone
        if (abs(humanX) > DEADZONE || abs(humanY) > DEADZONE) {
            Mouse.move(humanX, humanY, 0);
            Serial.print("Mouse moved: ");
            Serial.print(humanX);
            Serial.print(", ");
            Serial.println(humanY);
        } else {
            Serial.println("Movement below deadzone, ignored");
        }
    }
};

MouseRptParser Prs;

void setup() {
    Serial.begin(BAUD_RATE);
    Mouse.begin();
    
    Serial.println("Initializing USB Host Shield...");
    if (Usb.Init() == -1) {
        Serial.println("USB Host Shield failed to start!");
        while (1); // Halt
    }
    Serial.println("USB Host Shield initialized successfully.");
    HidMouse.SetReportParser(0, &Prs);
    Serial.println("Mouse parser set up. Ready.");
}

void loop() {
    Usb.Task();
    
    // Check for serial commands from Java
    if (Serial.available() > 0) {
        String data = Serial.readStringUntil('\n');
        Serial.print("Received serial data: ");
        Serial.println(data);
        
        int commaPos = data.indexOf(',');
        
        if (commaPos > 0) {
            aimX = data.substring(0, commaPos).toInt();
            aimY = data.substring(commaPos + 1).toInt();
            
            Serial.print("Parsed aimbot: X=");
            Serial.print(aimX);
            Serial.print(", Y=");
            Serial.println(aimY);
            
            // Activate if significant
            aimbotActive = (abs(aimX) > 0 || abs(aimY) > 0);
            Serial.print("Aimbot active: ");
            Serial.println(aimbotActive ? "YES" : "NO");
        } else {
            Serial.println("Invalid serial data format");
        }
    }
}