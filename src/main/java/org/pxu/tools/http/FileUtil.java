package org.pxu.tools.http;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FileUtil {

	/**
	 * Load a file from disk
	 * 
	 * @param file
	 * @return file content
	 */
	public static String loadFile(String file) {
		BufferedReader br = null;
		StringBuilder fileContent = new StringBuilder();
		try {

			String sCurrentLine;

			br = new BufferedReader(new FileReader(file));

			while ((sCurrentLine = br.readLine()) != null) {
				fileContent.append(sCurrentLine);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return fileContent.toString();
	}

}