package data;

import java.io.Serializable;

public abstract class Input implements Serializable {
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;
	private String				name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
