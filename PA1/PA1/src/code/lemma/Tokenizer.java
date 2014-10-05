package code.lemma;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

/**
 * @author Steven Hu, stevenhh@brandeis.edu
 * @author Georg Konwisser, gekonwi@brandeis.edu
 */
public class Tokenizer {

	private final StanfordCoreNLP pipeLine; // tool used for lemmatization
	private final Set<String> stopWords;

	private final static Pattern NOISE_PATTERN = buildNoisePattern();

	public Tokenizer() throws FileNotFoundException {
		// set up the Stanford Core NLP Tool
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma");
		pipeLine = new StanfordCoreNLP(props);

		// loading stop-words from file
		stopWords = new HashSet<String>();
		Scanner in = new Scanner(new FileReader("stopwords.txt"));
		while (in.hasNext())
			stopWords.add(in.next());
		in.close();
	}

	/**
	 * Cleans noise, splits into tokens, lemmatizes each token.
	 * 
	 * @param documentText
	 * @return how often each lemma appeared in the sentence
	 */
	public List<String> getLemmas(String documentText) {
		documentText = removeNoise(documentText);

		List<String> lemmas = lemmatize(documentText);

		return filterStopWords(lemmas);
	}

	public static String removeNoise(String documentText) {
		Matcher matcher = NOISE_PATTERN.matcher(documentText);
		documentText = matcher.replaceAll(" ").trim();

		// replace all multiple blanks by a single blank
		return documentText.replaceAll("\\s+", " ");
	}

	private List<String> filterStopWords(List<String> lemmas) {
		List<String> filtered = new ArrayList<>(lemmas.size());

		for (String lemma : lemmas)
			if (!stopWords.contains(lemma))
				filtered.add(lemma);

		return filtered;
	}

	public static Pattern buildNoisePattern() {
		List<String> patterns = buildNoisePatternParts();

		StringBuilder sb = new StringBuilder();
		for (String pattern : patterns)
			// group each pattern and separate with OR
			sb.append("(" + pattern + ")|");

		// remove last OR
		sb.deleteCharAt(sb.length() - 1);

		// successive separators should be treated as one separation match
		String regex = "(" + sb.toString() + ")+";

		// make sure the regex dot character includes line breaks
		// (e.g. for the multi-line info box)
		return Pattern.compile(regex, Pattern.DOTALL);
	}

	private static List<String> buildNoisePatternParts() {
		List<String> patterns = new ArrayList<>();

		// remove the whole InfoBox
		patterns.add("\\{\\{Infobox.*\\}\\}");

		// remove whole URLs
		patterns.add("((http(s)?:\\/\\/)|(www\\.))\\S+\\.\\S+");

		// leave only the description of a picture
		patterns.add("\\[\\[File:.+px\\|");

		// remove whole references
		patterns.add("<ref>.+</ref>");

		// TODO are these needed? if HTML is decoded while reading in, it's not.
		patterns.add("&lt"); // "<" in HTML encoding
		patterns.add("&gt"); // ">" in HTML encoding
		patterns.add("&amp"); // "&" in HTML encoding

		// '' for italic, ''' for bold, but preserve the single '
		patterns.add("''+");

		// Unwanted characters, separated by a blank.
		String chars = "\" ` ´ . , : ; ! ? ( ) [ ] { } < > = / | \\ % & # § $ _ - ~ * ° ^ +";
		chars += " s d"; // white space (s) and digits (s)

		for (String c : chars.split(" "))
			// escape to avoid mis-interpretation as a special regex character
			patterns.add("\\" + c);

		return patterns;
	}

	/**
	 * Lemmatizes each element. Inspiration from:
	 * http://stackoverflow.com/questions/1578062/lemmatization-java
	 * 
	 * @param documentText
	 * @return
	 */
	public List<String> lemmatize(String documentText) {
		List<String> lemmas = new ArrayList<>();

		Annotation document = new Annotation(documentText);
		this.pipeLine.annotate(document);

		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				String lemma = token.get(LemmaAnnotation.class).toLowerCase();
				lemmas.add(lemma);
			}
		}

		return lemmas;
	}
}