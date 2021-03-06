package csv;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.io.FileUtils;

import mappings.Mapping;
import mappings.MappingFactory;
import tools.Tools;

public class Merge {
	private String csvSeparator = ",";	
	private String currentDateAndTime;
	
	private Type mode;
	
	private String currentEntityType = "";
	
	private Mapping mapping;
	private HashSet<String> local_ids;
	int num_duplicates = 0;
	
	public enum Type{
		NODES, EDGES;
	}
	
	public Merge(String dataSource, Type mode){
		this.mode = mode;
		setCurrentDateAndTime();
		
		MappingFactory factory = new MappingFactory(dataSource);		
		mapping = factory.getMapping(csvSeparator, currentDateAndTime, mode);	
		local_ids = new HashSet<>();
	}
	
	private void setCurrentDateAndTime(){
		TimeZone timeZone = TimeZone.getTimeZone("UTC");
		Calendar calendar = Calendar.getInstance(timeZone);

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE MMM dd hh:mm:ss 'UTC' yyyy", Locale.US);
		simpleDateFormat.setTimeZone(timeZone);
		
		String time = simpleDateFormat.format(calendar.getTime());
		
		this.currentDateAndTime = time;
	}		

	
//	### merge csv files #########################################
	public void mergeCsvFiles(String csvDirPath, String csvOutputPath){
		HashSet<String> entityTypes = new HashSet<>();
		
		String csvOutputPathPrefix = csvOutputPath.substring(0, csvOutputPath.lastIndexOf("."));
		String csvOutputPathSuffix = csvOutputPath.substring(csvOutputPath.lastIndexOf("."));
		String currentTypeOutputPath;
		
		File[] files = Tools.getFilesFromDir(csvDirPath);
		String content, header;
		String[] fileColumns;
		String[] rows;
		
		String orderedRowContent = "", orderedRow = "";
		
		File outFile = null;

		
		if(this.mode == Type.EDGES){
			outFile = new File(csvOutputPath);
		}
		
		int i = 0;
		for(File inFile: files){
			i++;
			System.out.println("File: " + inFile.getName());
			try {
				content = FileUtils.readFileToString(inFile, "UTF-8");
				
				header = content.substring(0, content.indexOf("\n"));
				header = header.replace("\"", "");
				fileColumns = header.split(csvSeparator);
				
				content = content.substring(content.indexOf("\n") +1);
				rows = content.split("\n");
				
				try {					
					if(this.mode == Type.EDGES){									
//						add header if it's the first iteration
						if(i == 1)
							FileUtils.writeStringToFile(outFile, this.mapping.headerMap.get(mapping.getEdgeEntitiesNames()[0])  + "\n", "UTF-8");
					}
					
					for(String row: rows){
						if(this.mode == Type.NODES){
							orderedRow = getOrderedRow(row, fileColumns);
							orderedRowContent = orderedRow + "," + mapping.getLabel(this.currentEntityType) + "\n";
							currentTypeOutputPath = csvOutputPathPrefix + "_" + currentEntityType + csvOutputPathSuffix;
							outFile = new File(currentTypeOutputPath);
							
	//						add header if it's the first occurrence of the current entityType
							if(entityTypes.add(this.currentEntityType))
								FileUtils.writeStringToFile(outFile, this.mapping.headerMap.get(this.currentEntityType) + ",:LABEL" + "\n", "UTF-8");
						}
						else if(this.mode == Type.EDGES){
							orderedRow = getOrderedRow(row, fileColumns);
							orderedRowContent = orderedRow + "\n";							
						}
						
						if(orderedRow != null && !orderedRow.isEmpty())
							FileUtils.writeStringToFile(outFile, orderedRowContent, "UTF-8", true);		
					}				
				} catch (IOException e1) {
					e1.printStackTrace();
				}				
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if(num_duplicates > 0){
			System.out.println(num_duplicates + " lines were skipped, because of duplicate IDs!");
		}	
	}		
	
	private String removeQuotes(String text){		
		return text.replace("\"", "");
	}
	
	private String getOrderedRow(String unorderedRow, String[] fileHeader){
		CSVCleaner cleaner = new CSVCleaner();		
		ArrayList<String> rowElements = cleaner.getElements(unorderedRow);	
		HashMap<String, Integer> columnsWithIndexes;
		
		this.currentEntityType = this.mapping.getCurrentEntityType(fileHeader, rowElements);		
		columnsWithIndexes = this.mapping.columnIndexMap.get(this.currentEntityType);
		
				
		String[] orderedElements = Tools.getInitializedStringArray(columnsWithIndexes.size());
		Integer columnIndex;
		String local_id = "";
		
		for(int i = 0; i < fileHeader.length; i++){
			columnIndex = columnsWithIndexes.get(fileHeader[i].trim());
			if(columnIndex != null && columnIndex >= 0){
				if(columnIndex >= orderedElements.length)
				if(i >= rowElements.size()){
				}
				orderedElements[columnIndex] = rowElements.get(i).trim();
				
				if(orderedElements[columnIndex].equals("null"))
					orderedElements[columnIndex] = "\"\"";
				
				if(fileHeader[i].contains(this.mapping.getLocalIDFieldName(this.currentEntityType))){
					orderedElements[columnIndex] = removeQuotes(orderedElements[columnIndex]);
					local_id = orderedElements[columnIndex];
					
//					don't allow duplicates
					if(this.mode == Type.NODES && !local_ids.add(local_id)){
						num_duplicates++;
						return "";
					}					
				}
				
				if(this.mode == Type.EDGES){
					if(fileHeader[i].contains(":START_ID") || fileHeader[i].contains(":END_ID"))
						orderedElements[columnIndex] = removeQuotes(orderedElements[columnIndex]);
				}
			}
		}
		
		if(this.mode == Type.NODES)
			mapping.addResearchGraphValues(orderedElements, columnsWithIndexes, local_id, this.currentEntityType);
		else
			mapping.addRelationshipResearchGraphValues(orderedElements, columnsWithIndexes);
		
		
		return Tools.getArrayAsString(orderedElements, csvSeparator);
	}	
}
