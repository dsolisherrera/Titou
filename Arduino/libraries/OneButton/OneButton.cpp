#include "OneButton.h"
#include <TinkerKit.h>

// ----- Initialization and Default Values -----

OneButton::OneButton(int pin){
  _pin = pin;
  

  _clickTicks = 600;        // number of millisec that have to pass by before a click is detected.
  _pressTicks = 1000;       // number of millisec that have to pass by before a lonn button press is detected.
  _state = 0; // starting with state 0: waiting for button to be pressed
  _buttonReleased = LOW;
  _buttonPressed = HIGH;
}

// explicitely set the number of millisec that have to pass by before a click is detected.
void OneButton::setClickTicks(int ticks) { 
  _clickTicks = ticks;
}

// save function for click event
void OneButton::attachClick(callbackFunction newFunction){
  _clickFunc = newFunction;
}

// save function for doubleClick event
void OneButton::attachDoubleClick(callbackFunction newFunction){
  _doubleClickFunc = newFunction;
}

// save function for tripleClick event
void OneButton::attachTripleClick(callbackFunction newFunction){
  _tripleClickFunc = newFunction;
}

void OneButton::tick(void){

  // Detect the input information
  TKTouchSensor touch(_pin);
  int buttonLevel = touch.read();
  unsigned long now = millis(); // current (relative) time in msecs.

  // Implementation of the state machine
  if (_state == 0) { // waiting for menu pin being pressed.
    if (buttonLevel == _buttonPressed) {
      _state = 1; // step to state 1
      _startTime = now; // remember starting time
    }
  } 
  
  else if (_state == 1) { // waiting for menu pin being released.
    if (buttonLevel == _buttonReleased) {
      _state = 2; // step to state 2
    }
  } 
  
  else if (_state == 2) { // waiting for menu pin being pressed the second time or timeout.
    if (now > _startTime + _clickTicks) {
      // this was only a single short click
      if (_clickFunc) _clickFunc();
      _state = 0; // restart.
    } 
	else if (buttonLevel == _buttonPressed) {
      _state = 3; // step to state 3
    }
  }

  else if (_state == 3) { // waiting for menu pin being released.
    if (buttonLevel == _buttonReleased) {
      _state = 4; // step to state 2
    }
  } 
	
  else if (_state == 4) { // waiting for menu pin being pressed the second time or timeout.
    if (now > _startTime + _clickTicks) {
      // this was only a double click
      if (_doubleClickFunc) _doubleClickFunc();
      _state = 0; // restart.
    } 
	else if (buttonLevel == _buttonPressed) {
      _state = 5; // step to state 3
    }
  } 
  
  else if (_state == 5) { // waiting for menu pin being released.
    if (buttonLevel == _buttonReleased) {
      _state = 6; // step to state 2
    }
  } 
  
  else if (_state = 6) { // waiting for menu pin being released finally.
	if (now > _startTime + _clickTicks) {
      // this was only a double click
      if (_tripleClickFunc) _tripleClickFunc();
      _state = 0; // restart.
    }

  }
  
/*   else if (_state == 3) { // waiting for menu pin being released finally.
    if (sensorLevel <= _threshold) {
      // this was a 2 click sequence.
      if (_doubleClickFunc) _doubleClickFunc();
      _state = 0; // restart.
    } // if

  }  */
  
} 

