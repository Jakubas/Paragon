## Version 1.1
### Added:
- Added a tuber digger, that digs soil and drops it and keeps the Odd Tubers that it digs up. It moves east after it can dig
  anymore in a particular area, so make sure that your path to the east is clear before running for extended periods.
- Added inventory utlities

### Fixed:
- The 'X' button on windows that are created by the automations will now close the window and stop the automation prematurely

### Changed:
- Inventory.drink will now drink from buckets as well
- Refactored paragon.Utils.java into its own package haven.paragon.utils and seperated it into multiple
  appropriate classes.
 


## Version 1.0
### Added:
- Farming automation that automates harvesting fields of any size
- Patrol bot that walks around a path and plays a sound if an enemy is found, 
the user must provide the sound file in the build directory in sounds/alarm.wav
- A path generator for the patrol bot	
- A bunch of hooks and utilities used by the Patrol and Farming automation

### Changed: 
- Added a button to the main game interface that appears after logging in. Clicking on this 
button provides the user with a list of bots that they can run
- Modified files in the haven package so that I could access the data and classes that I need 
for my automations
