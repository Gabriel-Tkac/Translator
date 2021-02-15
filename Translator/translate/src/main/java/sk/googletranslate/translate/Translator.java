package sk.googletranslate.translate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

/**
 * Hello world!
 *
 */
public class Translator 
{
	
	static final String[] slovneDruhyMale = {"podstatné meno", "prídavné meno", "zámeno", "číslovka", "sloveso", "príslovka", "predložka", "spojka", "častica"};
	static final String[] slovneDruhy = {"Podstatné meno", "Prídavné meno", "Zámeno", "Číslovka", "Sloveso", "Príslovka", "Predložka", "Spojka", "Častica"};
	static final String[] slovneDruhyEN = {"Noun", "Adjective", "Pronoun", "Number", "Verb", "Adverb", "Preposition", "Conjunction", "Particle"};
	static final int timeoutS = 5000;
	static boolean getMeanings = true;
	
    public static void main( String[] args ) throws InterruptedException, IOException
    {
    	System.setProperty("webdriver.gecko.driver", "C://geckodriver.exe");
    	
    	
    	Set<String> inputWords = readExcel();
    	
        Set<Word> words = new HashSet<Word>();
        
        File log = new File("C:\\Users\\a808683\\Documents\\Log.log");
        FileWriter fw = new FileWriter(log);
        PrintWriter pw = new PrintWriter(fw);
        
        // TimeOut
        long start = System.currentTimeMillis();
        System.out.println("START ON: " + new Date());
        int timeout = timeoutS * 1000;
        
        boolean unprocessed = true;
        long timeElapsed = System.currentTimeMillis() - start;
        while ( (timeElapsed  < timeout) && unprocessed) {
        	timeElapsed = System.currentTimeMillis() - start;
        	System.out.println("Seconds since start: " + timeElapsed/1000);
        	Processing spr =  processInput(inputWords);
        	words.addAll(spr.words);
        	
        	try {
        		Thread.sleep(10000);
	        	Set<String> nespracovaneSlova = spr.unprocessed;
	        	if (nespracovaneSlova.size() > 0) {
	        		System.out.println("V predchadzajucom kole zostalo nespracovanych slov: " + nespracovaneSlova.size());
	        		inputWords = nespracovaneSlova;
	        		spr =  processInput(inputWords);
	        		nespracovaneSlova = spr.unprocessed;
	        		words.addAll(spr.words);
	        	}
	        	else
	        		unprocessed = false;
        	}
        	catch (Exception ex) {
        		;
        	}
        }
        
        if (unprocessed) {
	        List<String> zoradeneNeuspesne = new ArrayList<String>(inputWords);
	        Collections.sort(zoradeneNeuspesne);
	        for (String slovo: zoradeneNeuspesne)
	        	pw.println(slovo);
        }
        
        pw.close();
        
        try {
			createExcel(words);
			System.out.println("Total time elapsed: " + (int) (System.currentTimeMillis() - start)/1000 + " s");
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    static Processing processInput(Set<String> inputWords) {
    	Processing processing = new Processing();
    	
    	Set<Word> words = new HashSet<Word>();
    	Set<String> unprocessed = new HashSet<String>();
    	
    	WebDriver driver = new FirefoxDriver();
    	driver.get("https://translate.google.as/");
    	//driver.get("https://translate.google.com/?hl=sk#view=home&op=translate&sl=en&tl=sk");
        WebElement input = driver.findElement(By.xpath("/html/body/c-wiz/div/div[2]/c-wiz/div[2]/c-wiz/div[1]/div[2]/div[2]/c-wiz[1]/span/span/div/textarea"));
        
        for (String toTransalte: inputWords) {
        	
	        try {
	        	Thread.sleep(2000);
	        	input.clear();
	        	
		        Word slovo = setENMeanings(driver, input, toTransalte);
		        
		        if (getMeanings) {
		        
			        try {
			        	slovo = getExamples(driver, slovo);
			        }
			        catch (Exception e) {}
			        
			        slovo = setSKMeanings(driver, slovo);
		        }
		        
		        input.clear();
		        
		        words.add(slovo);
	        }
	        catch (Exception e) {
	        	System.out.println("Chyba pri spracovani slova " + toTransalte);
	        	if (!toTransalte.isBlank())
	        		unprocessed.add(toTransalte);
	        }
        }
        
        processing.unprocessed = unprocessed;
        processing.words = words;
        
        return processing;
    }
    
    static class Processing {
    	Set<Word> words = new HashSet<Word>();
    	Set<String> unprocessed = new HashSet<String>();
    }
    
    static Word setENMeanings(WebDriver driver, WebElement input, String toTranslate) throws InterruptedException {
    	input.sendKeys(toTranslate);
        Thread.sleep(1000);
        
        WebElement resultTab = driver.findElement(By.className("Sp3AF"));
        try {
        	List<WebElement> expands = resultTab.findElements(By.className("VK4HE"));
        	for (WebElement expand: expands)
        		expand.click();
        }
        catch (Exception e) {
        	
        }
        
        WebElement description = driver.findElement(By.xpath("/html/body/c-wiz/div/div[2]/c-wiz/div[2]/c-wiz/div[2]/c-wiz/section/div/div/div[1]/div[1]/div/div"));
		List<WebElement> inner = allElements(description);
        
        WebElement output = driver.findElement(By.xpath("/html/body/c-wiz/div/div[2]/c-wiz/div[2]/c-wiz/div[1]/div[2]/div[2]/c-wiz[2]/div[5]/div/div[1]"));
        String translation = output.getText();
        System.out.println("Translation: " + translation);
        
        Set<String> obsahy = new LinkedHashSet<>();
        
        Word word = new Word(toTranslate, translation);
        
        if (!getMeanings)
        	return word;
        
     // Word kinds - EN
        Map<String, Set<String>> wordKinds = new TreeMap<String, Set<String>>();
        String wordKind = "";
        
        for (WebElement element: inner) {
        	
        	if (element.getText() != null && !element.getText().isBlank()) {
        		
        		if (isWordKind(Arrays.asList(slovneDruhy), element.getText())) {
        			obsahy.add(element.getText());
        			wordKind = element.getText();
        		}
        		
        		if (element.getAttribute("class").equals("fw3eif") && element.getAttribute("lang") != null && !element.getAttribute("lang").isEmpty()) {
        			String meaning = element.getText();
        			Set<String> meanings = null;
        			if (wordKinds.containsKey(wordKind)) {
        				meanings = wordKinds.get(wordKind);
        			}
        			else {
        				meanings = new HashSet<String>();
        			}
        			meanings.add(meaning);
        			wordKinds.put(wordKind, meanings);
            	}
        	}
        }
        
        word.setWordKinds(wordKinds);
        
        return word;
    }
    
    static Word setSKMeanings(WebDriver driver, Word word) {
        WebElement descriptionSK = driver.findElement(By.xpath("/html/body/c-wiz/div/div[2]/c-wiz/div[2]/c-wiz/div[2]/c-wiz/section/div/div/div[2]/div/div/div[1]"));
     
        try {
        	WebElement expand = descriptionSK.findElements(By.className("VK4HE")).get(1);
        	expand.click();
	        expand.findElement(By.tagName("i")).click();
        }
        catch (Exception e) {
        	
        }
        
        List<WebElement> innerSK = allElements(descriptionSK);
        
        Map<String, Set<String>> wordKinds = new TreeMap<String, Set<String>>();
        String wordKind = "";
        for (WebElement element: innerSK) {
        	
        	if (element.getText() != null && !element.getText().isBlank()) {
        		if (isWordKind(Arrays.asList(slovneDruhy), element.getText()))
        			wordKind = element.getText();
        	}
        	if (element.getAttribute("class").equals("KnIHac")) {
        		element = element.findElement(By.className("kgnlhe"));
        		if (element.getAttribute("data-sl") != null && !element.getAttribute("data-sl").isEmpty() &&
            			element.getAttribute("data-sl").equals("sk") &&
            			element.getAttribute("role") != null && !element.getAttribute("role").isEmpty() &&
            			element.getAttribute("role").equals("button")) {
                		if (element.getText() != null && !element.getText().isBlank()) {
                			if (!wordKind.isEmpty()) {
                				Set<String> meanings = null;
                    			if (wordKinds.containsKey(wordKind)) {
                    				meanings = wordKinds.get(wordKind);
                    			}
                    			else {
                    				meanings = new HashSet<String>();
                    			}
                    			meanings.add(element.getText());
                    			wordKinds.put(wordKind, meanings);
                			}
                		}	
                	}
        	}
        }
        
        word.setWordKindsSK(wordKinds);
    	
    	return word;
    }
    
    static Word getExamples(WebDriver driver, Word slovo) {
    	Set<String> examples = new HashSet<String>(5);
    	WebElement examplesElement = driver.findElement(By.xpath("/html/body/c-wiz/div/div[2]/c-wiz/div[2]/c-wiz/div[2]/c-wiz/section/div/div/div[1]/div[2]/div/div[1]"));
    	List<WebElement> elements = examplesElement.findElements(By.tagName("html-blob"));
    	int pocet = 0;
    	for (WebElement element: elements) {
    		pocet++;
    		examples.add(element.getText());
    		if (pocet >= 5)
    			break;
    	}
    	
    	slovo.setPouzitie(examples);
    	return slovo;
    }
    
    static List<WebElement> allElements(WebElement element) {
    	List<WebElement> elements = new ArrayList<WebElement>();
    	List<WebElement> list = element.findElements(By.tagName("div"));
    	if (!list.isEmpty()) {
    		elements.addAll(list);
    		for (WebElement elementIn: list)
    			elements.addAll(allElements(elementIn));
    	}
    	
    	return elements;
    }
    
    static List<WebElement> allElementsSK(WebElement element) {
    	List<WebElement> elements = new ArrayList<WebElement>();
    	List<WebElement> list = element.findElements(By.tagName("span"));
    	if (!list.isEmpty()) {
    		elements.addAll(list);
    		for (WebElement elementIn: list)
    			elements.addAll(allElements(elementIn));
    	}
    	
    	return elements;
    }
    
    static boolean isWordKind(List<String> wordKinds, String word) {
    	boolean isWordKind = false;
    	word = word.trim();
    	for (String kind: slovneDruhy) {
    		if (word.equalsIgnoreCase(kind)) {
    			isWordKind = true;
    			break;
    		}
    	}
    	
    	return isWordKind;
    }
    
    static void createExcel(Set<Word> slova) throws IOException {
    	Workbook workbook = new XSSFWorkbook();
    	Sheet sheet = workbook.createSheet("Preklady");
    	
    	List<Word> sorted = new ArrayList<Word>(slova);
    	Collections.sort(sorted);
    	
    	int rowNum = 0;
    	for (Word slovo: sorted) {
    		Row rowInit = sheet.createRow(rowNum++);
    		rowInit.createCell(0).setCellValue(slovo.getWord());
    		rowInit.createCell(1).setCellValue(slovo.getTranslation());
    		
    		System.out.println("createExcel - processingWord " + slovo.getWord());
    		// Word kinds for every word
    		int index = 0;
    		for (String druh: slovneDruhyMale) {
    			
    			int druhovEN = 0;
    			int druhovSK = 0;
    			Set<String> druhySK = new LinkedHashSet<>();
    			Set<String> druhyEN = new LinkedHashSet<>();
    			if (slovo.getWordKinds().size() > 0 && slovo.getWordKinds().containsKey(druh)) {
    				druhyEN = slovo.getWordKinds().get(druh);
    				druhovEN = druhyEN.size();
    			}
    			if (slovo.getWordKindsSK().size() > 0 && slovo.getWordKindsSK().containsKey(druh)) {
    				druhySK = slovo.getWordKindsSK().get(druh);
    				druhovSK = druhySK.size();
    			}
    			int max = Math.max(druhovEN, druhovSK);
    			
    			if (max > 0) {
    				Row headerRow = sheet.createRow(rowNum++);
    				headerRow.createCell(0).setCellValue(slovneDruhy[index]);
    				for (int i = 0; i < max; i++) {
		    			Row row = sheet.createRow(rowNum++);
		    			if (!druhyEN.isEmpty())
		    				row.createCell(0).setCellValue(getValueIfExists(druhyEN, i));
		    			if (!druhySK.isEmpty())
		    				row.createCell(1).setCellValue(getValueIfExists(druhySK, i));
		    		}
    			}
    			index++;
    		}
    		if (slovo.getPouzitie().size() > 0) {
    			Row row = sheet.createRow(rowNum++);
    			row.createCell(0).setCellValue("Priklady pouzitia");
    			for (String pouzitie: slovo.getPouzitie()) {
    				Row rowPouz = sheet.createRow(rowNum++);
    				rowPouz.createCell(0).setCellValue(pouzitie);
    			}
    		}
    		
    		sheet.createRow(rowNum++);
    		sheet.createRow(rowNum++);
    	}
    	
    	FileOutputStream fileOut = new FileOutputStream("C:\\Users\\a808683\\Documents\\Preklady.xlsx");
    	workbook.write(fileOut);
    	fileOut.close();
    }
    
    static Set<String> readExcel() {
    	Set<String> slova = new LinkedHashSet<String>();
    	try {
			Workbook workbook = WorkbookFactory.create(new File("C:\\Users\\a808683\\Documents\\Zdroj.xlsx"));
			Sheet sheet = workbook.getSheetAt(0);
			for (Row row: sheet) {
	            for(Cell cell: row) {
	            	slova.add(cell.getStringCellValue());
	            }
	        }
			
		} catch (EncryptedDocumentException | InvalidFormatException | IOException e) {
			e.printStackTrace();
		}
    	
    	System.out.println("Input words: " + slova.size());
    	return slova;
    }
    
    static String getValueIfExists(Set<String> set, int i) {
    	if (set.size() < i)
    		return "";
    	else {
    		if (i == set.size())
    			return "";
    		ArrayList<String> list = new ArrayList<>(set);
    		return list.get(i);
    	}
    }
}

