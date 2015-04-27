package inpro.sphinx.frontend;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DoubleData;

public class FrontendTellTale extends BaseDataProcessor {

	long firstSampleTime = 0; // in Milliseconds
	
	@Override
	public Data getData() throws DataProcessingException {
		Data d = getPredecessor().getData();
		if (d instanceof DoubleData) {
			firstSampleTime = ((DoubleData) d).getCollectTime();
		}
		return d;
	}

	/** in milliseconds */
	public long getFirstTimeSeen() {
		return firstSampleTime;
	}

}
