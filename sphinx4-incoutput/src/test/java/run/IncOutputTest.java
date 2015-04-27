package run;

import org.junit.Test;

public class IncOutputTest {

	@Test
	public void test() {
		try {
			IncOutput.main("file:src/test/resources/run/10001-90210-01803-8khz.wav", "/tmp/10001-90210-01803-8khz.inc_reco");
			IncOutput.main("file:src/test/resources/run/10001-90210-01803.wav", "/tmp/10001-90210-01803.inc_reco");
		} catch(Exception e) {
			assert false : e;
		}
	}

}
