void setup() {
  Serial.begin(9600);
}

void loop() {
  
  int adc_MQ = analogRead(A6); //Lemos la salida analógica  del MQ
  float voltaje = adc_MQ * (5.0 / 1023.0); //Convertimos la lectura en un valor de voltaje
  float Rs=1000*((5-voltaje)/voltaje);  //Calculamos Rs con un RL de 1k
  double alcohol=0.4091*pow(Rs/5463, -1.497); // calculamos la concentración  de alcohol con la ecuación obtenida.
  Serial.print(String(alcohol)+"mg/L|");
  delay(300);
}
