package com.seassoon.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;


public class Demo {
	
	
	public static void main(String[] args) throws Exception {
//		FileOutputStream oFile = new FileOutputStream("config.properties", true);
//		Properties config = new Properties();
//		config.load(Demo.class.getClassLoader().getResourceAsStream("config.properties"));
//		config.setProperty("name", "test");
//		config.store(oFile, "aaaaaaaaaaaaaaaaaaaa");
//		oFile.close();
		
		
		
//		Properties prop = new Properties();
//		InputStream in = new BufferedInputStream(new FileInputStream("config.properties"));
//		prop.load(in);
		
		
		
	      Properties pros = new Properties();
	      pros.load(new FileInputStream(new File("config.properties")));
	      pros.setProperty("key1", "value1");
	      pros.setProperty("key2", "value2");
	      pros.store(new FileOutputStream(new File("config.properties")),"header");
		
		
	}
	
	
}

