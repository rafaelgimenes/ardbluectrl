/*
* Arduino Mega + Modulo Bluetooth BT0417C_datasheet
* Autor: Rafael Gimenes Leite
* Codigo Generico, funciona com app pra android desenvolvida por mim chamada ArdBluCtr
*/

//declarando variaveis
char entrada[20];
int i=0;
int entrada_len;
int inByte;
int last_inByte;
String linhaEntrada = String(20);
int pino = 0;
void setup() {
   // usando 2 seriais para debug
   Serial.begin(9600);
   Serial1.begin(9600);
}

void loop() {
  //lendo da Serial1 bluetooh e mandando pra serial 0
  if (Serial1.available()) {
     inByte = Serial1.read();
     //Serial.print(inByte,BYTE);
     
     //reseta variavel linha Entrada, se achar o ! le o resto do Frame
     if (inByte == '!'){ 
       linhaEntrada="";
       read_frame();
     }
  
    //imprime o primeiro filtro
    Serial.println(linhaEntrada);
    //convertendo string pra inteiro
    String strPino = linhaEntrada.substring(linhaEntrada.indexOf("O")+2, linhaEntrada.indexOf("O")+4);
    String acao = linhaEntrada.substring(linhaEntrada.indexOf("O"), linhaEntrada.indexOf("O")+2);
    char this_char[strPino.length() + 1];
    strPino.toCharArray(this_char, (strPino.length() + 1));
    pino = atoi(this_char); 
    //fim conversao
    
    
    //agindo no PINO
    pinMode(pino,OUTPUT);
    if(acao=="ON")
      digitalWrite(pino,HIGH); 
    else if (acao=="OF")
      digitalWrite(pino,LOW); 
    
     
  }
   //lendo da Seiral0 e mandando pro Bluetooth
  if (Serial.available()) {
    int inByte = Serial.read();
    Serial1.print(inByte, BYTE); 
  }
}

void read_frame()  
{
  while (inByte!= '&') //fim da linha
   if (Serial1.available() > 0)
   { 
           inByte = Serial1.read();
           if(inByte!='&')
           entrada[entrada_len] = inByte;
           linhaEntrada += entrada[entrada_len];
           entrada_len++;
    }
    
}


