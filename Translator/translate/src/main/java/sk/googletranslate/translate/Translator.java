package sk.googletranslate.translate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFFont;
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
	static final int timeoutS = 1200;
	static boolean getMeanings = false;
	
    public static void main( String[] args ) throws InterruptedException, IOException
    {
    	System.setProperty("webdriver.gecko.driver", "C://geckodriver.exe");
    	WebDriver driver = new FirefoxDriver();
    	driver.get("https://translate.google.as/");
    	//driver.get("https://translate.google.com/?hl=sk#view=home&op=translate&sl=en&tl=sk");
        
    	Set<String> inputWords = readWords("C:\\Users\\a808683\\Documents");
    	
    	File log = new File("C:\\Users\\a808683\\Documents\\Log.log");
        FileWriter fw = new FileWriter(log);
        PrintWriter pw = new PrintWriter(fw);
        
        Set<Word> words = new LinkedHashSet<Word>();
        
        long start = System.currentTimeMillis();
        
        int processedWords = 0;
        for (String inputWord: inputWords) {
        	processedWords++;
        	System.out.println("Spracuvame slovo v poradi: " + processedWords);
        	
        	try {
        		Thread.sleep(1000);
        		
        		Word word = processInput(inputWord,  driver);
        		words.add(word);
        	}
        	catch (Exception e) {
        		pw.println(inputWord);
        		System.out.println("Slovo " + inputWord + " sa nespracovalo spravne");;
        	}
        	
        	if ((processedWords % 100) == 0) {
            	try {
            		createExcel(words, processedWords);
            		words.clear();
        			System.out.println("Total time elapsed: " + (int) (System.currentTimeMillis() - start)/1000 + " s");
        		} catch (IOException e) {
        			e.printStackTrace();
        		}
            }
        }
        
        pw.close();
        
    }
    
    static Word processInput(String inputWord, WebDriver driver) throws InterruptedException {
    	
    	Thread.sleep(2000);
    	
    	String transl = translate(inputWord, driver);
    	if (transl.contains("verified_user"))
    		transl = transl.replace("verified_user", "");
        Word word = new Word(inputWord, transl);
        
        Map<String, Set<String>> wordKinds = new TreeMap<String, Set<String>>();
        Set<String> examples = new LinkedHashSet<String>();
        try {
        	wordKinds = getWordKinds(driver);
        }
        catch (Exception e) {}
        try {
        	examples = getExamples(driver);
        }
        catch (Exception e) {}
        try {
        	if (!wordKinds.isEmpty()) {
        		Map<String, Map<String, String>> wordKindsMap = new TreeMap<String, Map<String, String>>();
        		for (String kind: wordKinds.keySet()) {
        			Map<String, String> preklady = new TreeMap<String, String>();
        			Set<String> examplesSet = wordKinds.get(kind);
        			if (!examplesSet.isEmpty()) {
        				for (String example: examplesSet) {
        					String translation = translate(example, driver);
        					preklady.put(example, translation);
        				}
        			}
        			wordKindsMap.put(kind, preklady);
        		}
        		word.setWordKinds(wordKindsMap);
        	}
        }
        catch (Exception e) {}
        
        try {
        	if (!examples.isEmpty()) {
        		Map<String, String> examplesMap = new TreeMap<String, String>();
        		for (String example: examples) {
        			String translation = translate(example, driver);
        			examplesMap.put(example, translation);
        		}
        		word.setExamples(examplesMap);
        	}
        }
        catch (Exception e) {}
        
        return word;
    }
    
    static Map<String, Set<String>> getWordKinds(WebDriver driver) throws InterruptedException {
    	
        WebElement resultTab = driver.findElement(By.className("Sp3AF"));
    	try {
        	List<WebElement> expands = resultTab.findElements(By.className("VK4HE"));
        	for (WebElement expand: expands)
        		expand.click();
        }
        catch (Exception e) { }
    	
    	WebElement description = driver.findElement(By.xpath("/html/body/c-wiz/div/div[2]/c-wiz/div[2]/c-wiz/div[2]/c-wiz/section/div/div/div[1]/div[1]/div/div"));
    	List<WebElement> inner = allElements(description);
        
	     // Word kinds - EN
    	Map<String, Set<String>> wordKinds = new TreeMap<String, Set<String>>();
        String wordKind = "";
        
        for (WebElement element: inner) {
        	
        	if (element.getText() != null && !element.getText().isBlank()) {
        		
        		if (isWordKind(Arrays.asList(slovneDruhy), element.getText())) {
        			wordKind = element.getText();
        		}
        		
        		if (element.getAttribute("class").equals("MZgjEb") &&
    				element.getAttribute("lang") != null && !element.getAttribute("lang").isEmpty()) {
        			String example = element.getText();
        			Set<String> examples = null;
        			if (wordKinds.containsKey(wordKind)) {
        				examples = wordKinds.get(wordKind);
        			}
        			else {
        				examples = new HashSet<String>();
        			}
        			examples.add(example);
        			wordKinds.put(wordKind, examples);
            	}
        	}
        }
        
        return wordKinds;
    }
    
    
    static Set<String> getExamples(WebDriver driver) {
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
    	return examples;
    }
    
    static String translate(String toTranslate, WebDriver driver) throws InterruptedException {
    	String translation = "";
    	try {
    		WebElement input = driver.findElement(By.xpath("/html/body/c-wiz/div/div[2]/c-wiz/div[2]/c-wiz/div[1]/div[2]/div[2]/c-wiz[1]/span/span/div/textarea"));
    		input.sendKeys(toTranslate);
    		Thread.sleep(2000);
    		
    		WebElement output = driver.findElement(By.xpath("/html/body/c-wiz/div/div[2]/c-wiz/div[2]/c-wiz/div[1]/div[2]/div[2]/c-wiz[2]/div[5]/div/div[1]"));
	    	
	        translation = output.getText();
	        input.clear();
    	}
    	catch (Exception e) {}
    	return translation;
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
    
    static void createExcel(Set<Word> slova, int order) throws IOException {
    	Workbook workbook = new XSSFWorkbook();
    	Sheet sheet = workbook.createSheet("Preklady");
    	

    	Font font = workbook.createFont();
        font.setFontHeightInPoints((short)10);
        font.setFontName("Arial");
        font.setColor(IndexedColors.BLACK.getIndex());
        font.setBold(true);

        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setFont(font);
    	
    	List<Word> sorted = new ArrayList<Word>(slova);
    	Collections.sort(sorted);
    	
    	int rowNum = 0;
    	for (Word word: sorted) {
    		Row rowInit = sheet.createRow(rowNum++);
    		Cell c1 = rowInit.createCell(0);
    		c1.setCellStyle(cellStyle);
    		c1.setCellValue(word.getWord());
    		Cell c2 = rowInit.createCell(1);
    		c2.setCellStyle(cellStyle);
    		c2.setCellValue(word.getTranslation());
    		
    		System.out.println("createExcel - processingWord " + word.getWord());
    		// Word kinds for every word
    		
    		if (!word.getWordKinds().isEmpty()) {
    			Map<String, Map<String, String>> kindsMap = word.getWordKinds();
    			Set<String> kinds = kindsMap.keySet();
    			for (String kind: kinds) {
    				Row kindRow = sheet.createRow(rowNum++);
    				kindRow.createCell(0).setCellValue(kind);
    				Map<String, String> exampleMap = kindsMap.get(kind);
    				if (!exampleMap.isEmpty()) {
    					for (String example: exampleMap.keySet()) {
    						Row exampleRow = sheet.createRow(rowNum++);
    						exampleRow.createCell(0).setCellValue(example);
    						exampleRow.createCell(1).setCellValue(exampleMap.get(example));
    					}
    				}
    			}
    		}
    		
    		if (word.getExamples().size() > 0) {
    			Row row = sheet.createRow(rowNum++);
    			row.createCell(0).setCellValue("Priklady pouzitia");
    			Map<String, String> examples = word.getExamples();
    			for (String example: examples.keySet()) {
    				Row rowPouz = sheet.createRow(rowNum++);
    				rowPouz.createCell(0).setCellValue(example);
    				rowPouz.createCell(1).setCellValue(examples.get(example));
    			}
    		}
    		
    		sheet.createRow(rowNum++);
    		sheet.createRow(rowNum++);
    	}
    	
    	String name = "C:\\Users\\a808683\\Documents\\Preklady" + order + ".xlsx";
    	FileOutputStream fileOut = new FileOutputStream(name);
    	workbook.write(fileOut);
    	fileOut.close();
    }
    
    static Set<String> readExcel(File xslx) {
    	Set<String> slova = new LinkedHashSet<String>();
    	try {
			Workbook workbook = WorkbookFactory.create(xslx);
			Sheet sheet = workbook.getSheetAt(0);
			for (Row row: sheet) {
	            for(Cell cell: row) {
	            	slova.add(cell.getStringCellValue());
	            }
	        }
			
		} catch (EncryptedDocumentException | InvalidFormatException | IOException e) {
			e.printStackTrace();
		}
    	
    	return slova;
    }
    
    static Set<String> readWords(String adresary) {
    	Set<String> words = new LinkedHashSet<String>();
    	File[] excels = new File(adresary).listFiles();
    	for (File xlsx: excels) {
    		if (xlsx.getName().contains("xlsx"))
    			words.addAll(readExcel(xlsx));
    	}
    	System.out.println("Input words: " + words.size());
    	return words;
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

