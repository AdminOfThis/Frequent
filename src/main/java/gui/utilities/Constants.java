package gui.utilities;

import java.util.Objects;

import control.ASIOController;
import data.Channel;
import javafx.util.StringConverter;

public final class Constants {

	public static final double				FFT_MIN				= -90;

	public static StringConverter<Channel>	CHANNEL_CONVERTER	= new StringConverter<Channel>() {

																	@Override
																	public Channel fromString(final String string) {
																		if (ASIOController.getInstance() != null) {
																			for (Channel c : ASIOController.getInstance().getInputList()) {
																				if (Objects.equals(c.getName(), string)) {
																					return c;
																				}
																			}
																		}
																		return null;
																	}

																	@Override
																	public String toString(final Channel object) {
																		if (object == null) {
																			return "- NONE -";
																		}
																		return object.getName();
																	}
																};

}
