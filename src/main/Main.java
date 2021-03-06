package main;
import java.io.File;

import csv.CSVCleaner;
import csv.Merge;
import csv.Merge.Type;

public class Main {
	
	public Main(){
		
	}
	
	public enum Source{
		GESIS,
		NIH
	}
	
	private void cleanData(String inputDirectoryPath, String outputDirectoryPath){
		CSVCleaner cleaner = new CSVCleaner();
		cleaner.cleanCsvFiles(inputDirectoryPath, outputDirectoryPath);
	}
	
	private void mergeFiles(String intputDirectoryPath, String outputFilePath, String dataSource, Type importType){
		Merge merge = new Merge(dataSource, importType);
		merge.mergeCsvFiles(intputDirectoryPath, outputFilePath);
	}
	
	private void deleteDirectory(String path){
		File file = new File(path);
		
		if(file.exists()){
			if(file.isDirectory()){
				for(File subFile: file.listFiles()){
					subFile.delete();
				}
			}
			file.delete();
		}			
	}
	
//	importSource = GESIS or NIH
	private static void startProcess(String csvDirPath, String outputFilePath, String importSource, String importType){
		Main main = new Main();
		
		Type type = Type.valueOf(importType);
		String cleanedCsvDirPath = "";
		
//		if(source == Source.GESIS){
			cleanedCsvDirPath = outputFilePath + "_cleaned/";
			
			System.out.println("Preparing data for import...");
			main.cleanData(csvDirPath, cleanedCsvDirPath);
			System.out.println("Done.");			

			csvDirPath = cleanedCsvDirPath;
//		}		
			
	
		System.out.println("Create csv-file for import...");
		main.mergeFiles(csvDirPath, outputFilePath, importSource, type);
		System.out.println("Done.");
	
			
//		if(source == Source.GESIS){
			System.out.println("Cleanup...");
//			main.deleteDirectory(csvDirPath);$$$$
			main.deleteDirectory(cleanedCsvDirPath);
			System.out.println("Finished.");
//		}
	}
	
	/*
	 * args[0] = directory of CSV files
	 * args[1] = output file path
	 * args[2] = import source ("GESIS" or "NIH")
	 * args[3] = import type ("NODES" or "EDGES")
	 */
	public static void main(String[] args){
		if(args != null && args.length == 4){
			double startTime = System.currentTimeMillis();
			startProcess(args[0], args[1], args[2], args[3]);
			double endTime = System.currentTimeMillis();
			System.out.println("Runtime: " + (endTime - startTime)/(1000*60) + " minutes");
		}
	}
}
