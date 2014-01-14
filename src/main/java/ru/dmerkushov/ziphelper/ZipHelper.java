/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.dmerkushov.ziphelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import ru.dmerkushov.loghelper.LoggerWrapper;

/**
 * A helper class to help use small ZIP files that can fit in the RAM completely. For such files, use of streaming interface provided by Java is excessive, so this provides a simple direct-access interface.
 * @author Dmitriy Merkushov
 */
public class ZipHelper {

	Map<ZipEntry, byte[]> entriesBytes;
	LoggerWrapper loggerWrapper = LoggerWrapper.getLoggerWrapper ("ru.dmerkushov.ziphelper.ZipHelper");

	/**
	 * Create an empty instance
	 *
	 * @throws java.io.FileNotFoundException
	 * @throws java.io.IOException
	 */
	public ZipHelper () throws FileNotFoundException, IOException {
		this (null);
	}

	/**
	 * Create an instance from a ZIP file. The file is unzipped during processing, so it may come to be too large.
	 *
	 * @param file if null, an empty instance is created
	 * @throws java.io.FileNotFoundException
	 * @throws java.io.IOException
	 */
	public ZipHelper (File file) throws FileNotFoundException, IOException {
		loggerWrapper.configureByDefaultSizeRolling (System.getProperty ("ZipHelperLogFile", "log/ZipHelper_%d_%u.log"));
		loggerWrapper.entering (file);
		
		entriesBytes = new HashMap<> ();

		if (file != null && file.exists ()) {
			ZipInputStream zipInputStream = new ZipInputStream (new FileInputStream (file));

			byte[] buffer = new byte[2048];
			ZipEntry zipEntry;
			while ((zipEntry = zipInputStream.getNextEntry ()) != null) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream ();
				int len;
				while ((len = zipInputStream.read (buffer)) > 0) {
					baos.write (buffer, 0, len);
				}
				entriesBytes.put (zipEntry, baos.toByteArray ());
			}
		}
		loggerWrapper.exiting ();
	}

	/**
	 * Get a list of this instance's entries
	 *
	 * @return
	 */
	public synchronized List<ZipEntry> getEntriesList () {
		loggerWrapper.entering ();
		
		List<ZipEntry> entriesList = new ArrayList<> ();

		for (ZipEntry entry : entriesBytes.keySet ()) {
			entriesList.add (entry);
		}

		loggerWrapper.exiting (entriesList);
		return entriesList;
	}

	/**
	 * Get the byte array of an entry
	 *
	 * @param entry
	 * @return the byte array, or null if the entry doesn;t exist
	 */
	public synchronized byte[] getEntryBytes (ZipEntry entry) {
		loggerWrapper.entering (entry);
		
		byte[] entryBytes = null;
		if (entriesBytes.containsKey (entry)) {
			entryBytes = entriesBytes.get (entry);
		}
		
		loggerWrapper.exiting (entryBytes);
		return entryBytes;
	}

	/**
	 * Add or update an entry
	 *
	 * @param entry
	 * @param bytes
	 */
	public synchronized void putEntry (ZipEntry entry, byte[] bytes) {
		loggerWrapper.entering (entry, bytes);
		
		entriesBytes.put (entry, bytes);
		
		loggerWrapper.exiting ();
	}

	/**
	 * Remove an entry
	 *
	 * @param entry
	 */
	public synchronized void removeEntry (ZipEntry entry) {
		loggerWrapper.entering (entry);
		
		entriesBytes.remove (entry);

		loggerWrapper.exiting ();
	}

	/**
	 * Save as ZIP file
	 *
	 * @param file if null, a new temp file is created
	 * @return the ZIP file
	 * @throws java.io.FileNotFoundException
	 * @throws java.io.IOException
	 */
	public synchronized File saveZip (File file) throws FileNotFoundException, IOException {
		loggerWrapper.entering (file);

		if (file == null) {
			file = File.createTempFile ("ZipHelper_", ".zip");
			loggerWrapper.info ("File was null, have created temp file " + file.getAbsolutePath ());
		}
		if (!file.exists ()) {
			file.createNewFile ();
			loggerWrapper.info ("File " + file.getAbsolutePath () + " did not exist, have created");
		}
		if (!file.canWrite ()) {
			throw new IOException ("Cannot write to " + file.getAbsolutePath ());
		}

		try (FileOutputStream fos = new FileOutputStream (file)) {
			try (ZipOutputStream zos = new ZipOutputStream (fos)) {
				for (ZipEntry entry : entriesBytes.keySet ()) {
					byte[] entryBytes = entriesBytes.get (entry);
					zos.putNextEntry (entry);
					zos.write (entryBytes);
				}
			} catch (IOException ex) {
				loggerWrapper.throwing (ex);
				throw ex;
			}
		}

		loggerWrapper.exiting (file);
		return file;
	}

}
