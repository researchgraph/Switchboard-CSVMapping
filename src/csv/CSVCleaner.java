package csv;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;

public class CSVCleaner {
	private String csvSeperator = ",";
	private int numColumns = 0;
	
	public CSVCleaner(){
		
	}
	
	public void cleanCsvFiles(String csvDirPath, String outpath){
		File csvDir = new File(csvDirPath);
		File[] files = csvDir.listFiles();		
		
		for(File file: files){
			System.out.println("File: " + file.getName());
			cleanCsvFile(file, new File(outpath + file.getName()));
		}		
	}
	
	public void cleanCsvFile(File inFile, File outFile){
		try {
			String content, header;
			content = FileUtils.readFileToString(inFile, "UTF-8");
			
			header = content.substring(0,  content.indexOf("\n"));
			FileUtils.writeStringToFile(outFile, header, "UTF-8");
			
			numColumns = header.split(csvSeperator).length;
			content = content.substring(content.indexOf("\n") +1);
			
			String[] lines = content.split("\n");
			cleanLines(lines, outFile);
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void cleanLines(String[] lines, File outFile) throws IOException{
		String content = "";
		for(int i = 0; i < lines.length; i++){
			String line = lines[i];
			ArrayList<String> elements = getElements(line);
			
			String currentLine = line;		
			while(elements.size() < this.numColumns && i < lines.length - 1){
				currentLine += " " + lines[++i];
				elements = getElements(currentLine);
			}
			content = cleanLine(elements);
			FileUtils.writeStringToFile(outFile, "\n" + content, "UTF-8", true);
		}
	}
	
	public String cleanLine(String line){
		ArrayList<String> elements = getElements(line);
		return cleanLine(elements);
	}
	
	public String cleanLine(ArrayList<String> elements){		
		try {
			if(elements.size() != this.numColumns)
				throw new Exception();
		} catch (Exception e) {
			System.out.println("Illegal number of columns:" + numColumns + "->" + elements.size());
			e.printStackTrace();
			for(String element: elements){
				System.out.println(element);
			}
		}		
		
		String line = "";
		String cleanElement;
		for(String element: elements){
			cleanElement = cleanElement(element);
			line += csvSeperator + cleanElement;
		}
		
//		line = listToLine(elements);		
		
		return line.substring(1);
	}
	
	public void setCsvSeparator(String csvSeparator){
		this.csvSeperator = csvSeparator;
	}
	
	private String cleanElement(String element){
		
		if(element.contains("\"")){
//			boolean addQuotes = false;
//			
//			if(element.startsWith("\"") && element.endsWith("\""))
//				addQuotes = true;			
			
			element = element.replace("\"", "");
			
//			if(addQuotes)
//				element = "\"" + element + "\"";
			
			element = "\"" + element + "\"";
		}		
		
		return element;		
	}
	
	private void mergeQuotes(ArrayList<String> elements, String lastQuote){
		String tmp = "";
		String element;
		boolean lastQuoteAdded = false;
		
		for(int i = elements.size()-1; i >= 0; i--){
			element = elements.get(i);
//			merge with last element that ends with quotes
			if(element.endsWith("\"") && endsWithUnevenQuotes(element)){
//				ending quote is inconsistency
				if(consistentQuotes(element)){
					elements.add(lastQuote);
					lastQuoteAdded = true;
				}
				else{
					element = element.substring(0, element.length()-1);
//					add elements between lastQuote and last element that ends with quotes
					if(!tmp.isEmpty())
						element += csvSeperator + tmp;
					element += csvSeperator + lastQuote;
					elements.set(i, element);	
					
//					remove remainder (tmp)
					while(elements.size() > i + 1){
						elements.remove(elements.size()-1);
					}
				}
				lastQuoteAdded = true;
				break;
			}
//			ending quote is inconsistency
			else if(element.isEmpty()){
				elements.add(lastQuote);
				lastQuoteAdded = true;
				break;
			}
			else{
				tmp = element + csvSeperator + tmp;
			}				
		}
		
		if(!lastQuoteAdded)
			elements.add(lastQuote);
	}
	
	private boolean consistentQuotes(String text){
		char quote = '"';
		int numQuotes = 0;
		
		for(int i = 0; i < text.length(); i++){
			if(text.charAt(i) == quote)
				numQuotes++;
		}
		
		return (numQuotes % 2 == 0);
	}
	
	private boolean endsWithUnevenQuotes(String text){
		int numTrailingQuotes = 0;
		boolean lastCharacterIsQuote = true;
		char quote = '"';
		for(int i = text.length()-1; i >= 0 && lastCharacterIsQuote; i--){
			if(text.charAt(i) == quote){
				numTrailingQuotes++;				
			}
			else{
				lastCharacterIsQuote = false;
			}
		}
//		if the last character is a quote, text only contained quotes
		if(lastCharacterIsQuote)
			return true;
		
		return (numTrailingQuotes % 2 != 0);
	}
	
	private boolean elementWithQuotesIsComplete(String text){
		text = text.replaceAll("[^\"]*", "");
		return (text.length() % 2 == 0);
	}
	
	
	
	public ArrayList<String> getElements(String line){
		ArrayList<String> elements = new ArrayList<>();
		line = line.trim();
		
		String[] separation = line.split(csvSeperator, 100000);
		String prefix = "";
		String tmp;
		for(String element: separation){	
			element = element.trim();
			if(prefix.isEmpty()){
				if(element.startsWith("\"")){
					if(element.endsWith("\"") && element.length() > 1 && endsWithUnevenQuotes(element))
						elements.add(element);
					else
						prefix = element;
				}
				else if(element.endsWith("\"") && !element.startsWith("\"") && endsWithUnevenQuotes(element)){
					mergeQuotes(elements, element);
				}
				else
					elements.add(element);
			}
			else{
				if(element.endsWith("\"") && endsWithUnevenQuotes(element)){
					tmp = prefix + csvSeperator + element;
					if(elementWithQuotesIsComplete(tmp)){
						elements.add(prefix + csvSeperator + element);
						prefix = "";
					}
					else{
						prefix += csvSeperator + element;
					}
				}	
				else
					prefix += csvSeperator + element;
			}
		}
		
		if(!prefix.isEmpty())
			elements.add(prefix);
		
		return elements;
	}
}
