#include <SoftwareSerial.h>// import the serial library

SoftwareSerial Genotronex(7, 8); // RX, TX
int vcc1=9;
//int vcc2=10;

void setup() {
  Genotronex.begin(9600);
  pinMode(vcc1, OUTPUT);
 // pinMode(vcc2, OUTPUT);
  digitalWrite(vcc1,HIGH);
  Serial.begin(9600);
}

void loop() {
  
  int adc_MQ = analogRead(A6); //Lemos la salida analógica  del MQ
  delay(100);
  float voltaje = adc_MQ * (5.0 / 1023.0); //Convertimos la lectura en un valor de voltaje
  float Rs=1000*((5-voltaje)/voltaje);  //Calculamos Rs con un RL de 1k
  double alcohol=0.4091*pow(Rs/5463, -1.497); // calculamos la concentración  de alcohol con la ecuación obtenida.
  Serial.println(String(alcohol)+"mg/L|");
  Genotronex.println(String(alcohol)+"mg/L");
  
}
