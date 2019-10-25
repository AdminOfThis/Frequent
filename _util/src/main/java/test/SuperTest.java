package test;

import org.junit.jupiter.api.BeforeAll;

import debug.Util;

public class SuperTest {

	@BeforeAll
	public static void disableSyso() {
		Util.disableSyso();
	}
}
