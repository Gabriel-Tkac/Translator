package sk.googletranslate.translate;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Word implements Comparable<Word> {
	
	private String word;
	private String translation;
	private Map<String, Set<String>> wordKinds = new TreeMap<String, Set<String>>();
	private Map<String, Set<String>> wordKindsSK = new TreeMap<String, Set<String>>();
	private Set<String> pouzitie = new HashSet<String>();
	
	public Word(String word, String translation) {
		this.word = word;
		this.translation = translation;
	}

	
	public String getWord() {
		return word;
	}


	public void setWord(String word) {
		this.word = word;
	}


	public String getTranslation() {
		return translation;
	}

	public void setTranslation(String translation) {
		this.translation = translation;
	}

	public Map<String, Set<String>> getWordKinds() {
		return wordKinds;
	}

	public void setWordKinds(Map<String, Set<String>> wordKinds) {
		this.wordKinds = wordKinds;
	}

	public Map<String, Set<String>> getWordKindsSK() {
		return wordKindsSK;
	}

	public void setWordKindsSK(Map<String, Set<String>> wordKindsSK) {
		this.wordKindsSK = wordKindsSK;
	}

	public Set<String> getPouzitie() {
		return pouzitie;
	}

	public void setPouzitie(Set<String> pouzitie) {
		this.pouzitie = pouzitie;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((translation == null) ? 0 : translation.hashCode());
		result = prime * result + ((word == null) ? 0 : word.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Word other = (Word) obj;
		if (translation == null) {
			if (other.translation != null)
				return false;
		} else if (!translation.equals(other.translation))
			return false;
		if (word == null) {
			if (other.word != null)
				return false;
		} else if (!word.equals(other.word))
			return false;
		return true;
	}

	@Override
	public int compareTo(Word o) {
		int lastCmp = word.compareTo(o.word);
        return (lastCmp != 0 ? lastCmp : translation.compareTo(o.translation));
	}

}
