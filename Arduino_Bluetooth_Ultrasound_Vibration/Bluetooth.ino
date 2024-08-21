#include <BluetoothSerial.h>

#define TRIG_PIN 23
#define ECHO_PIN 22
#define VIBRATION_PIN 18

BluetoothSerial SerialBT;

const int pwmChannel = 0;
const int pwmResolution = 8;
const int pwmFrequency = 1000;

unsigned long lastMeasurementTime = 0;
const unsigned long measurementInterval = 250; // 250 Millisekunden

bool measuring = false;

void setup() {
  Serial.begin(115200);
  SerialBT.begin("ESP32_Ultrasound");
  pinMode(TRIG_PIN, OUTPUT);
  pinMode(ECHO_PIN, INPUT);
  ledcSetup(pwmChannel, pwmFrequency, pwmResolution);
  ledcAttachPin(VIBRATION_PIN, pwmChannel);

  Serial.println("Bluetooth device is ready to pair");
}

void loop() {
  unsigned long currentTime = millis();

  if (SerialBT.available()) {
    String command = SerialBT.readString();
    command.trim();

    if (command == "start") {
      measuring = true;
      lastMeasurementTime = currentTime; // Start sofortige Messung
    } else if (command == "stop") {
      measuring = false;
      ledcWrite(pwmChannel, 0); // Stop vibration
    }
  }

  if (measuring) {
    // Check if it's time for the next measurement
    if (currentTime - lastMeasurementTime >= measurementInterval) {
      lastMeasurementTime = currentTime;

      // Start sending ultrasound data and activate vibration motor
      digitalWrite(TRIG_PIN, HIGH);
      delayMicroseconds(10);
      digitalWrite(TRIG_PIN, LOW);

      float duration_us = pulseIn(ECHO_PIN, HIGH);
      float distance_cm = 0.017 * duration_us;

      SerialBT.print(distance_cm);
      SerialBT.println(" cm");

      // Control vibration motor based on distance
      if (distance_cm > 0 && distance_cm <= 300) {
        if (distance_cm <= 30) {
          ledcWrite(pwmChannel, 255); // Start vibration
        } else {
          int vibrationDelay = map(distance_cm, 30, 200, 300, 1000);
          ledcWrite(pwmChannel, 255); // Vibrate
          delay(vibrationDelay);
          ledcWrite(pwmChannel, 0); // Stop vibration
          delay(vibrationDelay);
        }
      } else {
        ledcWrite(pwmChannel, 0); // Stop vibration
      }
    }
  }

  delay(100); // Kurzzeitiger Delay, um die CPU-Last zu reduzieren
}
