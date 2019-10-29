# Frequent
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/c07df06c627a4969b7b173b05bd326fa)](https://www.codacy.com/app/florianhild95/Frequent?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=AdminOfThis/Frequent&amp;utm_campaign=Badge_Grade) [![Build Status](https://travis-ci.com/AdminOfThis/Frequent.svg?branch=master)](https://travis-ci.com/AdminOfThis/Frequent) 

## Description

This is a lightweight audio analyzer, using the Steinberg ASIO protocol for spectrum analysis of a multichannel audio-stream.
The intended use-case of this application is to use in a live audio engineering setup, and therefore focuses on a lightweight installation, quick setup and operation, stability, as well as a great usability.


## Code
### JRE
This whole project is developed with, and for, an Oracle Java JRE8 Java installation.
Alltough possible that it could run with newer Java versions, since they don't include the JavaFX modules anymore, it would be a phuge pain to get it to work. 

Since the project mainly depends on the *jasiohost* library, which in turn uses its precompiled *.dll* files as library, the application is currently (and for the foreseeable future) only able to run on Windows machines.
### Dependencies
 - **JasioHost** library from [mhroth](https://github.com/mhroth) as the underlying ASIO library
 https://github.com/mhroth/jasiohost
	 - Fork from [wind-season](https://github.com/wind-season), which fixes a bug with DANTE Via and MADI-Connections
	 https://github.com/wind-season/jasiohost
 - **JavaFX** as the GUI library, displaying all data
 - **Log4j2** for handling logging to console and to file
 https://logging.apache.org/log4j/2.x/
 - **JUnit5** for testing the application, especially during the build
 https://junit.org/junit5/

### Build
The whole project is built with Maven. It currently conatains two submodules, and the main project. The submodules are:

 - **Util** A generic library which contains convenience methods for writing/loading properties, handling JavaFX problems, initializing Logging and much more.
 - **JavaVersionChecker** A simple mini project which checks the installed Java Version against a minimum requirement, to see if the programm can even start. Sits in an extra project, because it gets compiled for Java 1.6 to show an Error Message if neccessary.
 
After the main project is checked out, it is required to initialize and update the submodules with `git submodule --init` and `git submodule update --remote`.

The main project can then be built using `mvn clean compiler:compile compiler:testCompile validate surefire:test package`
It produces a "fat" *jar*, with all dependencies extracted into the jar, as well as a *.zip* file, which contains everything (except a Java8 runtime) required to run the programm.
Those artifacts can be found in the `./target` subdirectory.

## Usage
Start the program with the *start.bat*, or the *start_with_console.bat* script in the root directory of the *.zip* file.
### Input Chooser
You'll be required to choose the correct ASIO driver before the main application launches. 
![IO Chooser Window](https://i.postimg.cc/bJvncW72/grafik.png) 
### Main-Window
The main Application window has 3 logical regions:
 1.  **Channellist** 
 2.  **Menubar**
 3. **Module View**
![Main Window](https://i.postimg.cc/HkyRbskB/grafik.png)
### Channellist
The channellist on the left of the screen is used to display and select the input channels of the ASIO driver. It also shows a quick overview over the levels by acting as a vertical VU-meter.
The channels can be renamed, linked into stereo-pairs. and added/removed from groups via the context-menu accessible by right clicking any channel in the list.

By default there is also a live preview of the raw data of the selected channel on the bottom of the channel list. This can be paused for a closer look by clicking on the chart, swapped with a waveform-preview by double clicking or using the buttons under the chart, or completely removed by toggling the button above the channellist *Wave*.

### Modules
The content of the Module View can be choosen with the buttons on the top-right.
Currently there are 5 Modules to choose from:

| Module-Name | Description |
| --- | --- |
|*Spectrum* | The most used view, displays the frequency spectrum of the selected channel, using fourier transformation, in the range from *10Hz* to *20kHz* |
|*RTA* | Shows the frequency spectrum over time, by coloring more active frequency in a color-shade from yellow to red |
|*Groups* | Displays all the groups and their corresponding channels in one big overview|
|*Phase*|Shows the phase alignment of two channels, commonly used to see if stereo pairs, such as keyboards and guitars are out of phase |.
|*Bleed*| Shows the percentage of noise bleed of a selectable master signal into other channels signal. Can be used to check how much of the PA is audible in vocal or other mics. |
### Modules in Development
There are other modules, that can be accessed by starting the program with the flag `-debug`, but those are currently not considered useful.
