package data;

import java.io.Serializable;

/**
 * 
 * @author AdminOfThis
 * 
 *         Data Class for custom colors
 *
 */
public final class ColorEntry implements Serializable {

//	private static final Logger LOG = LogManager.getLogger(ColorEntry.class);

	private static final long serialVersionUID = 8517562797036670759L;
	private String name;
	private String entry;

	public ColorEntry(String name, String color) {
		this.name = name;
		this.entry = color;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEntry() {
		return entry;
	}

	public void setEntry(String entry) {
		this.entry = entry;
	}

}
