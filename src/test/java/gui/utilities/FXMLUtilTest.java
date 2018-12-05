package gui.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import javafx.scene.paint.Color;


class FXMLUtilTest {

	@Test
	void colorFade() {
		assertEquals(Color.RED, FXMLUtil.colorFade(0.0, Color.RED, Color.GREEN));
		assertEquals(Color.RED, FXMLUtil.colorFade(1.0, Color.GREEN, Color.RED));
		assertEquals(Color.rgb(255 / 2, 255 / 2, 255 / 2), FXMLUtil.colorFade(0.5, Color.WHITE, Color.BLACK));
		assertEquals(Color.RED, FXMLUtil.colorFade(1.0, Color.GREEN, Color.GREEN, Color.GREEN, Color.GREEN, Color.RED));
		assertEquals(Color.RED, FXMLUtil.colorFade(0.0, Color.RED, Color.GREEN, Color.GREEN, Color.GREEN, Color.GREEN));

	}

	@Test
	void toRGB() {
		assertEquals("#FF0000", FXMLUtil.toRGBCode(Color.RED));
		assertEquals("#FF0000", FXMLUtil.toRGBCode(Color.web("#FF0000")));
	}

}
