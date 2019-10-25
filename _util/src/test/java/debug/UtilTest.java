package debug;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import test.SuperTest;

class UtilTest extends SuperTest {

	@RepeatedTest(3)
	@Execution(ExecutionMode.CONCURRENT)
	void testExecuteTime() {

		int time = (int) ((Math.random() + 1.0) * 1000.0);
		Runnable e = () -> {
			try {
				Thread.sleep(time);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		};
		long result = Math.round(Util.executeTime(e) / 1000000.0);
		long min = time - 20;
		long max = time + 20;
		assertTrue(min <= result && result <= max);
	}

}
