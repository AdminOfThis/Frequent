# Frequent

[![Build Status](https://travis-ci.com/AdminOfThis/Frequent.svg?branch=master)](https://travis-ci.org/AdminOfThis/Frequent)

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/c07df06c627a4969b7b173b05bd326fa)](https://www.codacy.com/app/florianhild95/Frequent?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=AdminOfThis/Frequent&amp;utm_campaign=Badge_Grade)

This is a lightweight audio analyzer, using the Steinberg ASIO protocol for spectrum analysis of a multichannel audio-stream.
The intended use-case of this application is in a live audio engineering setup, and therefore focuses on lightweight, quick setup and operation, as well as a great usability.

The GUI uses JavaFX with some custom built elements to display the data.

Added some useful functions to access infos from church, since this is my primary use.


Dependencies

 - **JasioHost** library from [mhroth](https://github.com/mhroth) as the underlying ASIO library
 https://github.com/mhroth/jasiohost
	 - Fork from [wind-season](https://github.com/wind-season), which fixes a bug with DANTE Via
	 https://github.com/wind-season/jasiohost
 - **JavaFX** as the gui library
 - **Log4j2** for logging

Build
The whole project is built with Maven. It currently conatains two submodules, and the main project. The submodules are:

 - **Util** A generic library which contains convenience methods for writing/loading properties, handling JavaFX problems, initializing Logging and much more.
 - **JavaVersionChecker** A simple mini project which checks the installed Java Version against a minimum requirement, to see if the programm can even start. Sits in an extra project, because it gets compiled for Java 1.6 to show an Error Message if neccessary.

After the main project is checked out, it is required to initialize and update the submodules with `git submodule --init` and `git submodule update --remote`.





