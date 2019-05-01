module frequent {

	exports dialog;
	exports data;
	exports gui.utilities;
	exports control;
	exports control.bpmdetect;
	exports gui.utilities.controller;
	exports main;
	exports gui.preloader;
	exports gui.controller;
	exports gui.pausable;

	opens fxml;
	opens fxml.utilities;
	opens fxml.preloader;
	opens css;
	opens gui.preloader;
	opens gui.controller;
	opens gui.pausable;
	opens gui.utilities;
	opens gui.utilities.controller;
	opens logo;
	opens main;
	opens control;
	opens control.bpmdetect;
	opens data;
	opens dialog;

	requires transitive util;
	requires transitive org.apache.logging.log4j;
	requires transitive JTransforms;
	requires transitive jasiohost;
	requires transitive java.desktop;
}