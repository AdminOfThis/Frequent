

# Frequent
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/c07df06c627a4969b7b173b05bd326fa)](https://www.codacy.com/app/florianhild95/Frequent?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=AdminOfThis/Frequent&amp;utm_campaign=Badge_Grade) [![Build Status](https://travis-ci.com/AdminOfThis/Frequent.svg?branch=master)](https://travis-ci.com/AdminOfThis/Frequent) 

## Description

This is a lightweight audio analyzer, using the Steinberg ASIO protocol for spectrum analysis of a multichannel audio-stream.
The intended use-case of this application is in a live audio engineering setup, and therefore focuses on lightweight, quick setup and operation, as well as a great usability.

The GUI uses JavaFX with some custom built elements to display the data.

Added some useful functions to access infos from church, since this is my primary use.

## Code
### Dependencies
 - **JasioHost** library from [mhroth](https://github.com/mhroth) as the underlying ASIO library
 https://github.com/mhroth/jasiohost
	 - Fork from [wind-season](https://github.com/wind-season), which fixes a bug with DANTE Via and MADI-Connections
	 https://github.com/wind-season/jasiohost
 - **JavaFX** as the GUI library, displaying all data
 - **Log4j2** for logging to Console and to file

### Build
The whole project is built with Maven. It currently conatains two submodules, and the main project. The submodules are:

 - **Util** A generic library which contains convenience methods for writing/loading properties, handling JavaFX problems, initializing Logging and much more.
 - **JavaVersionChecker** A simple mini project which checks the installed Java Version against a minimum requirement, to see if the programm can even start. Sits in an extra project, because it gets compiled for Java 1.6 to show an Error Message if neccessary.

After the main project is checked out, it is required to initialize and update the submodules with `git submodule --init` and `git submodule update --remote`.
## Usage
Start the program with the *start.bat*, or the *start_with_console.bat* script in the root directory of the *.zip* file.
You'll be required to choose the correct ASIO driver before the main application launches. 
![IO Chooser Window](https://i.postimg.cc/bJvncW72/grafik.png) 
The main Application window has 3 logical regions:

 1.  **Channellist** 
 2.  **Menubar**
 3. **Module View**
![Main Window](https://i.postimg.cc/HkyRbskB/grafik.png)
The content of the Module View can be choosen with the buttons on the top-right.
Currently there are 5 Modules to choose from:

| Module-Name | Description |
| --- | --- |
|*Spectrum* | The most used view, displays the frequency spectrum of the selected channel, using fourier transformation, in the range from *10Hz* to *20kHz* |
|*RTA* | Shows the frequency spectrum over time, by coloring more active frequency in a color-shade from yellow to red |
|*Groups* | Displays all the groups and their corresponding channels in one big overview|
|*Phase*|Shows the phase alignment of two channels, commonly used to see if stereo pairs, such as keyboards and guitars are out of phase |.
|*Bleed*| Shows the percentage of noise bleed of a selectable master signal into other channels signal. Can be used to check how much of the PA is audible in vocal or other mics. |
