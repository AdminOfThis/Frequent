package data.util;

import javafx.application.Preloader.ProgressNotification;

public class StringProgressNotification extends ProgressNotification {

	private String message = "";

	public StringProgressNotification(double progress, String message) {
		super(progress);
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(final String mes) {
		this.message = mes;
	}

}
