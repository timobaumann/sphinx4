/*
 * 
 * Copyright 2007 InPro Project, Timo Baumann  
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package inpro.sphinx.instrumentation;

import inpro.sphinx.frontend.FrontendTellTale;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.recognizer.StateListener;
import edu.cmu.sphinx.recognizer.Recognizer.State;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.WordResult;
import edu.cmu.sphinx.decoder.ResultListener;
import edu.cmu.sphinx.util.TimeFrame;
import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.instrumentation.Monitor;
import edu.cmu.sphinx.instrumentation.Resetable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Boolean;
import edu.cmu.sphinx.util.props.S4Component;
import edu.cmu.sphinx.util.props.S4Integer;
import edu.cmu.sphinx.util.props.S4String;

/**
 * inspects the current results and writes the phone-alignment to stdout or to a file
 */
public class LabelWriter implements Configurable,
						            ResultListener,
						            Resetable,
						            StateListener,
									Monitor  {
    /**
     * A Sphinx property that defines which recognizer to monitor
     */
	@S4Component(type = Recognizer.class)
    public final static String PROP_RECOGNIZER = "recognizer";
	@S4Boolean(defaultValue = false)
    public final static String PROP_INTERMEDIATE_RESULTS = "intermediateResults";
	@S4Boolean(defaultValue = true)
    public final static String PROP_FINAL_RESULT = "finalResult";
	@S4Integer(defaultValue = 1)
	public final static String PROP_STEP_WIDTH = "step";
    @S4Boolean(defaultValue = false)
    public final static String PROP_WORD_ALIGNMENT = "wordAlignment";
    @S4Boolean(defaultValue = false)
    public final static String PROP_PHONE_ALIGNMENT = "phoneAlignment";
    @S4Boolean(defaultValue = false)
    public final static String PROP_FILE_OUTPUT = "fileOutput";
    @S4String(defaultValue = "")
    public final static String PROP_FILE_BASE_NAME = "fileBaseName";    
	@S4Integer(defaultValue = 1)
    public final static String PROP_N_BEST = "nBest";
	
	@S4Component(type = FrontendTellTale.class)
	public final static String PROP_TELLTALE = "telltale";

    @S4Integer(defaultValue = 0)
	public final static String PROP_FIXED_LAG = "fixedLag";
	int fixedLag;
	
    // ------------------------------
    // Configuration data
    // ------------------------------
    private Recognizer recognizer = null;
    
    protected boolean intermediateResults = false;
    protected boolean finalResult = true;
    
    private boolean fileOutput = false;
    private String fileBaseName = "";
    
//    private int nBest = 1;
    
    protected boolean wordAlignment = true;
//    protected boolean phoneAlignment = true;
    
    private PrintStream wordAlignmentStream;
//    private PrintStream phoneAlignmentStream;

    FrontendTellTale ftt;

    /**
     * counts the number of recognition steps
     */
    protected int step = 0;
    protected int stepWidth = 1;
    
    private final List<WordResult> committedWords = new ArrayList<WordResult>();
    
    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        Recognizer newRecognizer = (Recognizer) ps.getComponent(PROP_RECOGNIZER);
        
        if (recognizer == null) {
            recognizer = newRecognizer;
            recognizer.addResultListener(this);
            recognizer.addStateListener(this);
        } else if (recognizer != newRecognizer) {
            recognizer.removeResultListener(this);
            recognizer.removeStateListener(this);
            recognizer = newRecognizer;
            recognizer.addResultListener(this);
            recognizer.addStateListener(this);
        }
        
        ftt = (FrontendTellTale) ps.getComponent(PROP_TELLTALE);
        
        intermediateResults = ps.getBoolean(PROP_INTERMEDIATE_RESULTS);
        finalResult = ps.getBoolean(PROP_FINAL_RESULT);
        
        fileOutput = ps.getBoolean(PROP_FILE_OUTPUT);
        fileBaseName = ps.getString(PROP_FILE_BASE_NAME);
        
        stepWidth = ps.getInt(PROP_STEP_WIDTH);

        wordAlignment = ps.getBoolean(PROP_WORD_ALIGNMENT);
//        phoneAlignment = ps.getBoolean(PROP_PHONE_ALIGNMENT);

    	if (wordAlignment) {
    		wordAlignmentStream = setStream("wordalignment");
    	}
//    	if (phoneAlignment) {
//    		phoneAlignmentStream = setStream("phonealignment");
//    	}
//        nBest = ps.getInt(PROP_N_BEST);
//		fixedLag = ps.getInt(PROP_FIXED_LAG);
    }

    

    /*
     * (non-Javadoc)
     * @see edu.cmu.sphinx.instrumentation.Resetable
     */
    public void reset() {
    	step = 0;
    }
    
    @SuppressWarnings("resource")
	private PrintStream setStream(String extension) {
    	PrintStream output = null;
    	if (fileOutput) {
			String filename = fileBaseName + "." + extension;
			try {
				output = new PrintStream(filename); 
			} catch (FileNotFoundException e) {
				System.err.println("Unable to open file " + filename);
				output = System.out;
			}
    	} else {
    		output = System.out;
    	}
    	return output;
    }

    /**
     * convert a token list to an alignment string 
     * 
     * @param tokens list of tokens
     * /
    public static String tokenListToAlignment(List<Token> tokens, int lastFrame) {
		StringBuilder sb = new StringBuilder();
		if (tokens.size() > 0) {
			boolean tokenFollowsData = ResultUtil.hasWordTokensLast(tokens.get(0));
			ListIterator<Token> tokenIt = tokens.listIterator();
			// iterate over the list and print the associated times
	        while (tokenIt.hasNext()) {
	            Token token = tokenIt.next();
	            int startFrame;
	            int endFrame;
	            if (tokenFollowsData) {
	            	if (tokenIt.hasPrevious()) {
		            	startFrame = tokenIt.previous().getFrameNumber();
		            	tokenIt.next();
	            	} else { startFrame = 0; }
	            	endFrame = token.getFrameNumber();
	            } else {
	            	startFrame = token.getFrameNumber();
	            	if (tokenIt.hasNext()) {
		            	endFrame = tokenIt.next().getFrameNumber();
		            	tokenIt.previous();
	            	} else break;
	            }
	            sb.append(String.format(Locale.US, "%.2f", startFrame * TimeUtil.FRAME_TO_SECOND_FACTOR)); // a frame always lasts 10ms 
	            sb.append("\t");
	            sb.append(String.format(Locale.US, "%.2f", endFrame * TimeUtil.FRAME_TO_SECOND_FACTOR)); // dito
	            sb.append("\t");
	            // depending on whether word, filler or other, dump the string-representation
	            SearchState state = token.getSearchState();
	            sb.append(ResultUtil.stringForSearchState(state)); 
	            sb.append("\n");
	            if (endFrame > lastFrame)
	            	break;
	        }
		}
        return sb.toString();
    } */

    /**
     * write labels of aligned segments or words  
     * to stream &lt;!--if it differs from lastAlignment--&gt;
     * @param list
     * @param stream
     * @param timestamp
     *
    private String writeAlignment(List<Token> list, PrintStream stream, boolean timestamp) {
    	String alignment = tokenListToAlignment(list, timestamp ? step - fixedLag : Integer.MAX_VALUE);
		if (timestamp) {
			stream.print("Time: ");
			stream.println(step / 100.0);
		}
		if (alignment.equals("")) {
			stream.println("0.0\t" + step / 100.0 + "\t<sil>\n");
		} else {
			stream.println(alignment);
		}
    	return alignment;
    } */

    /** a frame lasts 0.01 seconds (= 10 milliseconds) */
    public static double FRAME_TO_SECOND_FACTOR = 0.01;
	public static double MILLISECOND_TO_SECOND_FACTOR = 0.001;

    /*
     * @see edu.cmu.sphinx.result.ResultListener#newResult(edu.cmu.sphinx.result.Result)
     */
	public void newResult(Result result) {
    	if ((intermediateResults == !result.isFinal()) 
    	|| (finalResult && result.isFinal())) {
    		//boolean timestamp = !result.isFinal();
    		List<WordResult> words = result.getTimedBestResult(true);
    		if (!words.isEmpty()) {
    			
    			wordAlignmentStream.printf(Locale.US, "Time: %.2f\n", ftt.getFirstTimeSeen() * MILLISECOND_TO_SECOND_FACTOR);
    			for (WordResult wr : committedWords) {
    				TimeFrame tf = wr.getTimeFrame();
                    wordAlignmentStream.printf(Locale.US, 
                                               "%.2f\t%.2f\t%s\n", 
                                               tf.getStart() * MILLISECOND_TO_SECOND_FACTOR, 
                                               tf.getEnd() * MILLISECOND_TO_SECOND_FACTOR, 
                                               wr.getWord().getSpelling());
    			}
    			for (WordResult wr : words) {
    				TimeFrame tf = wr.getTimeFrame();
                    wordAlignmentStream.printf(Locale.US, 
                                               "%.2f\t%.2f\t%s\n", 
                                               tf.getStart() * MILLISECOND_TO_SECOND_FACTOR, 
                                               tf.getEnd() * MILLISECOND_TO_SECOND_FACTOR, 
                                               wr.getWord().getSpelling());
    			}
    			wordAlignmentStream.println();
    		}
    		// create n-best list
/*    		List<Token> nBestList;
    		if (nBest == 1) {
    			nBestList = Collections.singletonList(result.getBestToken());
    		} else {
	    		nBestList = result.getResultTokens();
	    		if (nBestList.size() <= 0) {
	    			System.out.println("# reverting to active tokens...");
	    			nBestList = result.getActiveTokens().getTokens();
	    		}
	    		Collections.sort(nBestList, Token.COMPARATOR);
	    		if (nBestList.size() > nBest) {
	    			nBestList = nBestList.subList(0, nBest);
	    		}
    		}
    		// iterate through n-best list
    		Iterator<Token> nBestIt = nBestList.iterator();
    		while (nBestIt.hasNext()) {
    			Token nBestToken = nBestIt.next();
	    		if (wordAlignment) {
	    			List<Token> wordTokenList = ResultUtil.getTokenList(nBestToken, true, false);
	    			writeAlignment(wordTokenList, wordAlignmentStream, timestamp);
	    		}
	    		if (phoneAlignment) {
	    			List<Token> phoneTokenList = ResultUtil.getTokenList(nBestToken, false, true);
	    			writeAlignment(phoneTokenList, phoneAlignmentStream, timestamp);
	    		}
    		} */
    	}
    	if (result.isFinal()) {
    		committedWords.addAll(result.getTimedBestResult(true));
    	}
    }

	@Override
	public void statusChanged(State status) {
		if (status == State.RECOGNIZING) {
			step = 0;
		}
	}
    
}
