package sk.googletranslate.translate;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Word implements Comparable<Word> {
	
	private String slovo;
	private String preklad;
	private Map<String, Set<String>> slovneDruhy = new TreeMap<String, Set<String>>();
	private Map<String, Set<String>> slovneDruhySK = new TreeMap<String, Set<String>>();
	private Set<String> pouzitie = new HashSet<String>();
	
	public Word(String slovo, String preklad) {
		this.slovo = slovo;
		this.preklad = preklad;
	}

	public String getSlovo() {
		return slovo;
	}

	public void setSlovo(String slovo) {
		this.slovo = slovo;
	}

	public String getPreklad() {
		return preklad;
	}

	public void setPreklad(String preklad) {
		this.preklad = preklad;
	}

	public Map<String, Set<String>> getSlovneDruhy() {
		return slovneDruhy;
	}

	public Map<String, Set<String>> getSlovneDruhySK() {
		return slovneDruhySK;
	}

	public Set<String> getPouzitie() {
		return pouzitie;
	}
	
	public void setPouzitie(String pouzitieItem) {
		pouzitie.add(pouzitieItem);
	}
	
	public void setSlovneDruhy(Map<String, Set<String>> slovneDruhy) {
		this.slovneDruhy = slovneDruhy;
	}

	public void setSlovneDruhySK(Map<String, Set<String>> slovneDruhySK) {
		this.slovneDruhySK = slovneDruhySK;
	}

	public void setPouzitie(Set<String> pouzitie) {
		this.pouzitie = pouzitie;
	}

	public void setSlovnyDruh(String druh, String vyznam) {
		Set<String> vyznamy = slovneDruhy.get(druh) != null ? slovneDruhy.get(druh) : new HashSet<String>();
		vyznamy.add(vyznam);
		slovneDruhy.put(druh, vyznamy);
	}
	
	public void setSlovnyDruhSK(String druh, String vyznam) {
		Set<String> vyznamy = slovneDruhySK.get(druh) != null ? slovneDruhy.get(druh) : new HashSet<String>();
		vyznamy.add(vyznam);
		slovneDruhy.put(druh, vyznamy);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((preklad == null) ? 0 : preklad.hashCode());
		result = prime * result + ((slovo == null) ? 0 : slovo.hashCode());
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
		if (preklad == null) {
			if (other.preklad != null)
				return false;
		} else if (!preklad.equals(other.preklad))
			return false;
		if (slovo == null) {
			if (other.slovo != null)
				return false;
		} else if (!slovo.equals(other.slovo))
			return false;
		return true;
	}

	@Override
	public int compareTo(Word o) {
		int lastCmp = slovo.compareTo(o.slovo);
        return (lastCmp != 0 ? lastCmp : preklad.compareTo(o.preklad));
	}

}
