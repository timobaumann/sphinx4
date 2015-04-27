package run;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.Context;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;
import edu.cmu.sphinx.result.WordResult;

public class IncOutput {

	/**
	 * @param args
	 */
	public static void main(String... args) throws Exception {
        //String waveURL = "file:/home/timo/uni/experimente/073_iSRcomparison/incrasr-git/data/SWBD_chunks/4519/4519.2_A.16khz.wav";
        URL waveURL = new URL(args[0]);
        File incRecoURL = new File(args[1]);
        AudioInputStream ais = AudioSystem.getAudioInputStream(waveURL);
        int sampleRate = Math.round(ais.getFormat().getSampleRate());
        assert sampleRate == 8000 || sampleRate == 16000 : "No support for sample rate " + sampleRate;
        ais.close();
        InputStream stream = waveURL.openStream();
        stream.skip(44);
        
        System.out.println("Loading models...");

        Configuration configuration = new Configuration();
        configuration.setSampleRate(sampleRate);

        // Load model from the jar
        configuration
                .setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");

        // You can also load model from folder
        // configuration.setAcousticModelPath("file:en-us");

        configuration
                .setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        configuration
                .setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.dmp");

        Context context = new Context(configuration);
        context.setLocalProperty("labelWriter->fileName", incRecoURL);
        
        StreamSpeechRecognizer recognizer = new StreamSpeechRecognizer(context);

        // Simple recognition with generic model
        recognizer.startRecognition(stream);
        SpeechResult result;
        while ((result = recognizer.getResult()) != null) {

            System.out.format("Hypothesis: %s\n", result.getHypothesis());

            System.out.println("List of recognized words and their times:");
            for (WordResult r : result.getWords()) {
                System.out.println(r);
            }

            System.out.println("Best 3 hypothesis:");
            for (String s : result.getNbest(3))
                System.out.println(s);

        }
        recognizer.stopRecognition();
        stream.close();
	}

}
