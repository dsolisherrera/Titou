#include <SPI.h>
#include <SdFat.h>
#include <SdFatUtil.h>
#include <SFEMP3Shield.h>
#include <PinChangeInt.h>
#include <OneButton.h>

// Set debugging to true to get serial messages:

boolean debugging = false;

// Initial volume for the MP3 chip. 0 is the loudest, 255
// is the lowest.

unsigned char volume = 40;

// Start up playing:

boolean playing = true;

// Set loop_all to true if you would like to automatically
// start playing the next file after the current one ends:

boolean loop_all = true;

// LilyPad MP3 pin definitions:

#define TRIG1 A0
#define ROT_LEDG A1
#define SHDN_GPIO1 A2
#define ROT_B A3
#define TRIG2_SDA A4
#define TRIG3_SCL A5
#define RIGHT A6
#define LEFT A7

#define TRIG5_RXI 0
#define TRIG4_TXO 1
#define MP3_DREQ 2
#define ROT_A 3
#define ROT_SW 4
#define ROT_LEDB 5
#define MP3_CS 6
#define MP3_DCS 7
#define MP3_RST 8
#define SD_CS 9
#define ROT_LEDR 10
#define MOSI 11
#define MISO 12
#define SCK 13


// Global variables and flags for interrupt request functions:
char track[13];
char inChar;

volatile int BPM;                   // used to hold the pulse rate
volatile int Signal;                // holds the incoming raw data
volatile int IBI = 600;             // holds the time between beats, must be seeded! 
volatile boolean Pulse = false;     // true when pulse wave is high, false when it's low
volatile boolean QS = false; 

//Sensor
int pin_sensor = TRIG1;
// Touch Button
OneButton moment_button(ROT_LEDG);
OneButton audio_button(TRIG2_SDA);

// Library objects:

SdFat sd;
SdFile file;
SFEMP3Shield MP3player;


void setup(){
  
  byte result;
  Serial.begin(115200);
  if (debugging){
    Serial.begin(115200);
    // ('F' places constant strings in program flash to save RAM)
    Serial.println(F("Lilypad MP3 Player"));
    Serial.print(F("Free RAM = "));
    Serial.println(FreeRam(), DEC);
  }

  // Set up I/O pins:
  
  pinMode(MP3_CS, OUTPUT);
  pinMode(SHDN_GPIO1, OUTPUT);
  
  // switch is common anode with external pulldown, do not turn on pullup
  pinMode(MP3_DREQ, INPUT);
  pinMode(MP3_DCS, OUTPUT);
  pinMode(MP3_RST, OUTPUT);

  pinMode(SD_CS, OUTPUT);
  pinMode(MOSI, OUTPUT);
  pinMode(MISO, INPUT);
  pinMode(SCK, OUTPUT);
 
  moment_button.attachClick(moment_ClickFunction);  
  moment_button.attachDoubleClick(moment_DoubleClickFunction); 
    
  audio_button.attachClick(audio_ClickFunction);  
  audio_button.attachDoubleClick(audio_DoubleClickFunction);
  audio_button.attachTripleClick(audio_TripleClickFunction);
  //audio_button.setClickTicks(600);
  interruptSetup(); 
 
 // Turn off amplifier chip / turn on MP3 mode:

  digitalWrite(SHDN_GPIO1, LOW);

  // Initialize the SD card:

  if (debugging) Serial.println(F("Initializing SD card... "));

  result = sd.begin(SD_SEL, SPI_HALF_SPEED);

  if (result != 1) {
    if (debugging) Serial.println(F("error, halting"));
  }
  else 
    if (debugging) Serial.println(F("OK"));
 
  //Initialize the MP3 chip:
  
  if (debugging) Serial.println(F("Initializing MP3 chip... "));
  result = MP3player.begin();

  // Check result, 0 and 6 are OK:
  
  if((result != 0) && (result != 6)){
    if (debugging){
      Serial.print(F("error "));
      Serial.println(result);
    }
  }
  else
    if (debugging) Serial.println(F("OK"));
  
  // Get initial track:
  
  sd.chdir("/",true); // Index beginning of root directory
  getNextTrack();
  if (debugging){
    Serial.print(F("current track: "));
    Serial.println(track);
  }
  
  // Set initial volume (same for both left and right channels)
  
  MP3player.setVolume(volume, volume);
  
  // Uncomment to get a directory listing of the SD card:
  // sd.ls(LS_R | LS_DATE | LS_SIZE);

  // Turn on amplifier chip:
  digitalWrite(SHDN_GPIO1, HIGH);
  delay(2);
  startPlaying();
}

void loop(){
  
  audio_button.tick();
  moment_button.tick();
  // Handle "last track ended" situations
  // (should we play the next track?)
  
  // Are we in "playing" mode, and has the
  // current file ended?
  if (Serial.available()){
    inChar = Serial.read();
    if (inChar =='P'){
      audio_ClickFunction();
    }
    if (inChar =='N'){
      audio_DoubleClickFunction();
    }
    if (inChar =='B'){
      audio_TripleClickFunction();
    } 
  }
  
  if (playing && !MP3player.isPlaying()){
    
    getNextTrack(); // Set up for next track
    // If loop_all is true, start the next track
    if (loop_all){
      startPlaying();
    }
    else
      playing = false;
  }
  delay(10);
}

// this function will be called when the button was pressed 1 time and them some time has passed.
void audio_ClickFunction() {
  if (debugging) Serial.println(F("Audio - One click"));
  playing = !playing;
  if (playing)
    resumePlaying();
    //startPlaying();
  else
    //stopPlaying();
    pausePlaying();
} 

// this function will be called when the button was pressed 2 times in a short timeframe.
void audio_DoubleClickFunction(){
  if (debugging) Serial.println(F("Audio - Double click"));
  if (playing)
    stopPlaying();
  // Get the next file:
  getNextTrack();
  // If we were previously playing, let's start again!
  if (playing) startPlaying(); 
  if (debugging){
    Serial.print(F("current track "));
    Serial.println(track);
  }
} 

// this function will be called when the button was pressed 3 times in a short timeframe.
void audio_TripleClickFunction(){
  if (debugging) Serial.println(F("Audio - Triple click"));
  if (playing)
    stopPlaying();
  // Get the next file:
  getPrevTrack();
  // If we were previously playing, let's start again!
  if (playing) startPlaying(); 
  if (debugging){
    Serial.print(F("current track "));
    Serial.println(track);
  }
}

// this function will be called when the button was pressed 1 time and them some time has passed.
void moment_ClickFunction() {
  //if (debugging) Serial.println(F("Moment - One click"));
  Serial.print('M');
  Serial.print(BPM);
  Serial.print('\n');
} 

// this function will be called when the button was pressed 2 times in a short timeframe.
void moment_DoubleClickFunction(){
  //if (debugging) Serial.println(F("Moment - Double click"));
  Serial.print('C');
  Serial.print('\n');
} 


void changeVolume(boolean direction){
  // Increment or decrement the volume.
  // This is handled internally in the VS1053 MP3 chip.
  // Lower numbers are louder (0 is the loudest).
  
  if (volume < 255 && direction == false)
    volume += 2;
  
  if (volume > 0 && direction == true)
    volume -= 2;
  MP3player.setVolume(volume, volume);

  if (debugging){
    Serial.print(F("volume "));
    Serial.println(volume);
  }
}


void getNextTrack(){
  // Get the next playable track (check extension to be
  // sure it's an audio file)
  do
    getNextFile();
  while(isPlayable() != true);
}


void getPrevTrack(){
  // Get the previous playable track (check extension to be
  // sure it's an audio file)
  do
    getPrevFile();
  while(isPlayable() != true);
}


void getNextFile(){
  // Get the next file (which may be playable or not)
  int result = (file.openNext(sd.vwd(), O_READ));

  // If we're at the end of the directory,
  // loop around to the beginning:
  
  if (!result)
  {
    sd.chdir("/",true);
    getNextTrack();
    return;
  }
  file.getFilename(track);  
  file.close();
}


void getPrevFile(){
  // Get the previous file (which may be playable or not)
  
  char test[13], prev[13];

  // Getting the previous file is tricky, since you can
  // only go forward when reading directories.

  // To handle this, we'll save the name of the current
  // file, then keep reading all the files until we loop
  // back around to where we are. While doing this we're
  // saving the last file we looked at, so when we get
  // back to the current file, we'll return the previous
  // one.
  
  // Yeah, it's a pain.

  strcpy(test,track);

  do{
    strcpy(prev,track);
    getNextTrack();
  }
  while(strcasecmp(track,test) != 0);
  
  strcpy(track,prev);
}


void startPlaying(){
  int result;
 
  if (debugging){
    Serial.print(F("playing "));
    Serial.print(track);
    Serial.print(F("..."));
  }  

  result = MP3player.playMP3(track);
  if (debugging){
    Serial.print(F(" result "));
    Serial.println(result);  
  }
}

void stopPlaying(){
  if (debugging) Serial.println(F("stopping playback"));
  MP3player.stopTrack();
}

void pausePlaying(){
  if (debugging) Serial.println(F("pausing playback"));
  MP3player.pauseMusic();
}

void resumePlaying(){
  if (debugging) Serial.println(F("resuming playback"));
  MP3player.resumeMusic();
}

boolean isPlayable(){
  // Check to see if a filename has a "playable" extension.
  // This is to keep the VS1053 from locking up if it is sent
  // unplayable data.

  char *extension;
  
  extension = strrchr(track,'.');
  extension++;
  if (
    (strcasecmp(extension,"MP3") == 0) ||
    (strcasecmp(extension,"WAV") == 0) ||
    (strcasecmp(extension,"MID") == 0) ||
    (strcasecmp(extension,"MP4") == 0) ||
    (strcasecmp(extension,"WMA") == 0) ||
    (strcasecmp(extension,"FLA") == 0) ||
    (strcasecmp(extension,"OGG") == 0) ||
    (strcasecmp(extension,"AAC") == 0)
  )
    return true;
  else
    return false;
}

