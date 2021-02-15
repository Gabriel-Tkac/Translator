package sk.googletranslate.translate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
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
	
    public static void main( String[] args ) throws InterruptedException, IOException
    {
    	System.setProperty("webdriver.gecko.driver", "C://geckodriver.exe");
    	
    	Map<String, Boolean> spracovanieSlov = new TreeMap<String, Boolean>();
    	Set<String> zadania = readExcel();
    	for (String zadanie: zadania)
    		spracovanieSlov.put(zadanie, false);
    	
        Set<Word> slova = new HashSet<Word>();
        
        File log = new File("C:\\Users\\a808683\\Documents\\Log.log");
        FileWriter fw = new FileWriter(log);
        PrintWriter pw = new PrintWriter(fw);
        pw.println("Neuspesne zapisane slova: ");
        
        // TimeOut
        long start = System.currentTimeMillis();
        int timeout = 150 * 1000;
        
        boolean nespracovane = true;
        while ( ( (System.currentTimeMillis() - start) < timeout) && nespracovane) {
        	System.out.println("Od startu uplynulo: " + (System.currentTimeMillis() - start));
        	Spracovanie spr =  spracujSlova(zadania, pw);
        	slova.addAll(spr.slova);
        	
        	try {
        		Thread.sleep(10000);
	        	Set<String> nespracovaneSlova = spr.nespracovane;
	        	if (nespracovaneSlova.size() > 0) {
	        		zadania = nespracovaneSlova;
	        		spr =  spracujSlova(zadania, pw);
	        		nespracovaneSlova = spr.nespracovane;
	        		slova.addAll(spr.slova);
	        	}
	        	else
	        		nespracovane = false;
        	}
        	catch (Exception ex) {
        		nespracovane = false;
        	}
        }
        
        
        pw.close();
        
        try {
			createExcel(slova);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    static Spracovanie spracujSlova(Set<String> zadania, PrintWriter pw) {
    	Spracovanie spracovanie = new Spracovanie();
    	
    	Set<Word> slova = new HashSet<Word>();
    	Set<String> nespracovane = new HashSet<String>();
    	
    	WebDriver driver = new FirefoxDriver();
    	JavascriptExecutor js = (JavascriptExecutor) driver;
    	driver.get("https://translate.google.com/?hl=sk#view=home&op=translate&sl=en&tl=sk");
        WebElement input = driver.findElement(By.xpath("/html/body/c-wiz/div/div[2]/c-wiz/div[2]/c-wiz/div[1]/div[2]/div[2]/c-wiz[1]/span/span/div/textarea"));
        
        for (String toTransalte: zadania) {
        	
	        try {
	        	Thread.sleep(1000);
	        	
		        Word slovo = setENVyznamy(driver, input, toTransalte);
		        
		        try {
		        	slovo = getPriklady(driver, slovo);
		        }
		        catch (Exception e) {}
		        
		        slovo = setSKVyznamy(driver, slovo);	        
		        
		        input.clear();
		        
		        System.out.println("Slovo ma slovnych druhov: " + slovo.getSlovneDruhy().size());
		        slova.add(slovo);
	        }
	        catch (Exception e) {
	        	System.out.println("Chyba pri spracovani slova " + toTransalte);
	        	pw.println(toTransalte);
	        	if (!toTransalte.isBlank())
	        		nespracovane.add(toTransalte);
	        }
        }
        
        spracovanie.nespracovane = nespracovane;
        spracovanie.slova = slova;
        
        return spracovanie;
    }
    
    static class Spracovanie {
    	Set<Word> slova = new HashSet<Word>();
    	Set<String> nespracovane = new HashSet<String>();
    }
    
    static Word setENVyznamy(WebDriver driver, WebElement input, String toTranslate) throws InterruptedException {
    	input.sendKeys(toTranslate);
        System.out.println("Text v elemente " +input.getText());
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
        System.out.println("Z elementu sa nacitalo " + inner.size() + " elementov 'div'");
        
        WebElement output = driver.findElement(By.xpath("/html/body/c-wiz/div/div[2]/c-wiz/div[2]/c-wiz/div[1]/div[2]/div[2]/c-wiz[2]/div[5]/div/div[1]"));
        String preklad = output.getText();
        System.out.println("Preklad: " + preklad);
        
        Set<String> obsahy = new LinkedHashSet<>();
        
        Word slovo = new Word(toTranslate, preklad);
        
        // Slovne druhy - EN
        Map<String, Set<String>> slovneDruhySlova = new TreeMap<String, Set<String>>();
        String slovnyDruh = "";
        
        for (WebElement element: inner) {
        	
        	if (element.getText() != null && !element.getText().isBlank()) {
        		
        		if (jeSlovnyDruh(Arrays.asList(slovneDruhy), element.getText())) {
        			obsahy.add(element.getText());
        			slovnyDruh = element.getText();
        			//System.out.println("Nasiel sa slovny druh: " + slovnyDruh);
        			
        		}
        		
        		if (element.getAttribute("class").equals("fw3eif") && element.getAttribute("lang") != null && !element.getAttribute("lang").isEmpty()) {
        			String vyznam = element.getText();
        			//System.out.println("K tomuto slovnemu druhu mame vyznam: " + vyznam);
        			Set<String> vyznamy = null;
        			if (slovneDruhySlova.containsKey(slovnyDruh)) {
        				//System.out.println("Mapa uz obsahovala " + slovnyDruh);
        				vyznamy = slovneDruhySlova.get(slovnyDruh);
        				//System.out.println("Pocet vyznamov bol " + vyznamy.size());
        			}
        			else {
        				vyznamy = new HashSet<String>();
        			}
        			vyznamy.add(vyznam);
        			//System.out.println("Novy pocet vyznamov pre slovny druh " + slovnyDruh + " je " + vyznamy.size());
        			slovneDruhySlova.put(slovnyDruh, vyznamy);
        			//System.out.println("Aktualna velkost mapy: " + slovneDruhySlova.size());
            	}
        	}
        }
        
        System.out.println("Slovnik obsahuje slovnych druhov: " + slovneDruhySlova.size());
        slovo.setSlovneDruhy(slovneDruhySlova);
        
        return slovo;
    }
    
    static Word setSKVyznamy(WebDriver driver, Word slovo) {
        WebElement descriptionSK = driver.findElement(By.xpath("/html/body/c-wiz/div/div[2]/c-wiz/div[2]/c-wiz/div[2]/c-wiz/section/div/div/div[2]/div/div/div[1]"));
//        List<WebElement> slDr = driver.findElements(By.className("Nv4rrc"));
//        System.out.println("Nasiel som slovne druhy: " + slDr.size());
//        for (WebElement sl: slDr)
//        	System.out.println(sl.getText());
        
        //WebElement expand = driver.findElement(By.xpath("/html/body/c-wiz/div/div[2]/c-wiz/div[2]/c-wiz/div[2]/c-wiz/section/div/div/div[2]/div/div/div[2]/div[1]"));
        
        try {
        	WebElement expand = descriptionSK.findElements(By.className("VK4HE")).get(1);
        	System.out.println("Pokusame sa expandovat element " + expand.getText());
        	expand.click();
	        expand.findElement(By.tagName("i")).click();
        }
        catch (Exception e) {
        	
        }
        
        List<WebElement> innerSK = allElements(descriptionSK);
        System.out.println("Z elementu pre SK sa nacitalo " + innerSK.size() + " elementov 'div'");
        
        
        Map<String, Set<String>> slovneDruhySlova = new TreeMap<String, Set<String>>();
        String slovnyDruh = "";
        for (WebElement element: innerSK) {
        	
        	if (element.getText() != null && !element.getText().isBlank()) {
        		if (jeSlovnyDruh(Arrays.asList(slovneDruhy), element.getText()))
        			slovnyDruh = element.getText();
        	}
        	if (element.getAttribute("class").equals("KnIHac")) {
        		element = element.findElement(By.className("kgnlhe"));
        		if (element.getAttribute("data-sl") != null && !element.getAttribute("data-sl").isEmpty() &&
            			element.getAttribute("data-sl").equals("sk") &&
            			element.getAttribute("role") != null && !element.getAttribute("role").isEmpty() &&
            			element.getAttribute("role").equals("button")) {
                		if (element.getText() != null && !element.getText().isBlank()) {
                			if (!slovnyDruh.isEmpty()) {
                				Set<String> vyznamy = null;
                    			if (slovneDruhySlova.containsKey(slovnyDruh)) {
                    				//System.out.println("Mapa uz obsahovala " + slovnyDruh);
                    				vyznamy = slovneDruhySlova.get(slovnyDruh);
                    				//System.out.println("Pocet vyznamov bol " + vyznamy.size());
                    			}
                    			else {
                    				vyznamy = new HashSet<String>();
                    			}
                    			vyznamy.add(element.getText());
                    			//System.out.println("Novy pocet vyznamov pre slovny druh " + slovnyDruh + " je " + vyznamy.size());
                    			slovneDruhySlova.put(slovnyDruh, vyznamy);
                			}
                		}	
                	}
        	}
        }
        
        System.out.println("Slovnik obsahuje SLOVENSKYCH slovnych druhov: " + slovneDruhySlova.size());
        slovo.setSlovneDruhySK(slovneDruhySlova);
    	
    	return slovo;
    }
    
    static Word getPriklady(WebDriver driver, Word slovo) {
    	Set<String> priklady = new HashSet<String>(5);
    	WebElement examples = driver.findElement(By.xpath("/html/body/c-wiz/div/div[2]/c-wiz/div[2]/c-wiz/div[2]/c-wiz/section/div/div/div[1]/div[2]/div/div[1]"));
    	List<WebElement> elements = examples.findElements(By.tagName("html-blob"));
    	int pocet = 0;
    	for (WebElement element: elements) {
    		System.out.println("Mame priklad : " + element.getText());
    		pocet++;
    		priklady.add(element.getText());
    		if (pocet >= 5)
    			break;
    	}
    	
    	slovo.setPouzitie(priklady);
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
    
    static boolean jeSlovnyDruh(List<String> slovneDruhy, String slovo) {
    	boolean jeSlovnyDruh = false;
    	slovo = slovo.trim();
    	for (String druh: slovneDruhy) {
    		if (slovo.equalsIgnoreCase(druh)) {
    			jeSlovnyDruh = true;
    			break;
    		}
    	}
    	
    	return jeSlovnyDruh;
    }
    
    static void createExcel(Set<Word> slova) throws IOException {
    	Workbook workbook = new XSSFWorkbook();
    	Sheet sheet = workbook.createSheet("Preklady");
    	
    	
    	int rowNum = 0;
    	for (Word slovo: slova) {
    		Row rowInit = sheet.createRow(rowNum++);
    		rowInit.createCell(0).setCellValue(slovo.getSlovo());
    		rowInit.createCell(1).setCellValue(slovo.getPreklad());
    		
    		System.out.println("Vytvor Excel - spracuvame slovo " + slovo.getSlovo());
    		// Slovne druhy pre kazde slovo
    		int index = 0;
    		for (String druh: slovneDruhyMale) {
    			
    			int druhovEN = 0;
    			int druhovSK = 0;
    			Set<String> druhySK = new LinkedHashSet<>();
    			Set<String> druhyEN = new LinkedHashSet<>();
    			if (slovo.getSlovneDruhy().size() > 0 && slovo.getSlovneDruhy().containsKey(druh)) {
    				System.out.println("Nasiel sa slovny druh: " + druh);
    				druhyEN = slovo.getSlovneDruhy().get(druh);
    				druhovEN = druhyEN.size();
    				System.out.println("Toto slovo ma EN vyznamov pre slovny druh: " + druhovEN);
    			}
    			if (slovo.getSlovneDruhySK().size() > 0 && slovo.getSlovneDruhySK().containsKey(druh)) {
    				System.out.println("Nasiel sa slovny druh SK: " + druh);
    				druhySK = slovo.getSlovneDruhySK().get(druh);
    				druhovSK = druhySK.size();
    				System.out.println("Toto slovo ma SK vyznamov pre slovny druh: " + druhovSK);
    			}
    			int max = Math.max(druhovEN, druhovSK);
    			
    			if (max > 0) {
    				Row headerRow = sheet.createRow(rowNum++);
    				headerRow.createCell(0).setCellValue(slovneDruhy[index]);
    				//sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, 1));
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
    	
    	System.out.println("Nacitanych slov: " + slova.size());
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
    
    static boolean jeNespracovane(Map<String, Boolean> spracovanieSlov) {
    	boolean jeNespracovane = false;
    	Set<String> keys = spracovanieSlov.keySet();
    	for (String key: keys)
    		if (spracovanieSlov.get(key) == false)
    			return true;
    	
    	return jeNespracovane;
    }
}

