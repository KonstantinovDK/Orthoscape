package orthoscape;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public abstract class OrthoscapeHelpFunctions{
	
	static String loadUrl(String StrUrl){
		StringBuffer result = new StringBuffer();
		try{
			URL url = new URL(StrUrl);			
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();           		   
			connection.setRequestMethod("GET");
			connection.setDoOutput(true);
			connection.connect();
				       
			BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream(),"UTF8"));
			String urline;

			while ((urline = rd.readLine()) != null) {
				result.append(urline).append("\n");
			}
			connection.disconnect();
		    rd.close();
		} catch (MalformedURLException e) {
				System.out.println("Can't connect using URL: " + StrUrl);
				// Wait 5 minutes and try again
				try {
				    Thread.sleep(300000);                 // 5 min
				    loadUrl(StrUrl);
				} catch(InterruptedException ex) {
				    Thread.currentThread().interrupt();
				}
		//		System.exit(1);
		} catch (IOException e) {
				System.out.println("Can't get input stream from url: " + StrUrl);
				// Wait 5 minutes and try again
//				try {
//				    Thread.sleep(300000);                 // 5 min
//				    loadUrl(StrUrl);
//				} catch(InterruptedException ex) {
//				    Thread.currentThread().interrupt();
//				}
		//		System.exit(1);
		}
		return result.toString();
	}

	static String[] stringFounder(String strWhereFind, String strToFind){	
		String[] curlines = new String[2];

		while (strWhereFind.length() != 0){ //цикл до упоминания искомой строки
	   	   	curlines = strWhereFind.split("\n", 2);
		    if (curlines.length == 1){
		       	break;
		    }
		    strWhereFind = curlines[1];
		    if (curlines[0].contains(strToFind)){
		        break;
		    }        	        
		}
		return curlines;
	}

	static void singleFilePrinting(File file, String data){
		try {
	    	PrintStream outStream = new PrintStream(file.toString());
	    	outStream.println(data);
	    	outStream.close();	
		}catch (IOException e2){System.out.println("Can't write data to the file " + file.toString());}
	}

	static void doubleFilePrinting(File file, String data1, String data2){
	    try {
	    	PrintStream outStream = new PrintStream(file.toString());
	    	outStream.println(data1);
	    	outStream.println(data2);
	    	outStream.close();	
		}catch (IOException e2){System.out.println("Can't write data to the file " + file.toString());}
	}

	static void tripleFilePrinting(File file, String data1, String data2, String data3){
	    try {
	    	PrintStream outStream = new PrintStream(file.toString());
	    	outStream.println(data1);
	    	outStream.println(data2);
	    	outStream.println(data3);
	    	outStream.close();	
		}catch (IOException e2){System.out.println("Can't write data to the file " + file.toString());}
	}
	
	static void listFilePrinting(File file, List<String> data){
		try {
	    	PrintStream outStream = new PrintStream(file.toString());
	    	for (int z=0; z<data.size(); z++){
	    		outStream.println(data.get(z));							    		
	    	}
	    	outStream.close();	
		}catch (IOException e2){System.out.println("Can't write data to the file " + file.toString());}
	}

	static String completeFileReader(File file){
		String line;
		StringBuffer result = new StringBuffer();
		try{
			BufferedReader reader = new BufferedReader(new FileReader(file.toString()));
		    while ((line = reader.readLine()) != null) {
		    	result.append(line).append("\n");
		    }
		    reader.close();
		}catch (IOException e2){
			System.out.println("Can't read the file " + file.toString());
			System.exit(1);
		}
		return result.toString();
	}
	
	static void getListFiles(ArrayList<File> filesIntestDirectory, String str) {
        File f = new File(str);
        for (File s : f.listFiles()) {
            if (s.isFile()) {
            	filesIntestDirectory.add(s);
            } else if (s.isDirectory()) {
                getListFiles(filesIntestDirectory, s.getAbsolutePath());      
            }
        }	        
	}
}
