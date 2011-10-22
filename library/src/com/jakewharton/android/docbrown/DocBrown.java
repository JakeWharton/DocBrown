package com.jakewharton.android.docbrown;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import dalvik.system.DexFile;
import android.content.Context;
import android.os.Build;
import android.util.Log;

/**
 * Helper library for the conditional loading of classes from a dex file with
 * the primary goal of injecting classes from API levels newer than that of
 * the one on which your application is running.
 */
public final class DocBrown {
	/** Buffer size for copying from assets to internal storage. */
	private static final int BUFFER_SIZE = 8 * 1024;
	/** Logging tag. */
	private static final String TAG = "DocBrown";
	/** Folder name for internal storage of dex. */
	private static final String INTERNAL_DEX_FOLDER = "DocBrownDex";
	/** Folder name for internal cache of dex. */
	private static final String INTERNAL_CACHE_FOLDER = "DocBrownCache";
	
	/** Source file name of dex in assets. */
	private final String mSourceDex;
	/** List of conditional loads to attempt. */
	private final List<ConditionalLoad> mConditionalLoads;
	
	
	private DocBrown(String source) {
		mSourceDex = source;
		mConditionalLoads = new ArrayList<ConditionalLoad>();
	}
	
	
	/**
	 * Set the name of the dex file in the application assets from which the
	 * classes will be loaded.
	 * 
	 * @param source File name.
	 * @return {@link DocBrown} instance.
	 */
	public static DocBrown from(String source) {
		return new DocBrown(source);
	}
	
	/**
	 * Class name to load from the specified dex file.
	 * 
	 * @param classNames List of one or more fully qualified class names.
	 * @return A {@link ConditionalLoad} instance to allow specifying an API
	 * level condition to restrict loading.
	 */
	public ConditionalLoad load(String... classNames) {
		Set<String> classes = new HashSet<String>();
		for (String className : classNames) {
			// We replace dots with slashes for future folder-based lookups
			classes.add(className.replace('.', '/'));
		}
		ConditionalLoad conditinalLoad = new ConditionalLoad(this, classes);
		mConditionalLoads.add(conditinalLoad);
		return conditinalLoad;
	}
	
	/**
	 * Attempt to load all of the conditionals into the specified context.
	 * 
	 * @param context Context from which the assets, internal storage, and
	 * class loader will be used for loading.
	 */
	public void into(Context context) {
		// Check if the dex is loaded in internal storage
		File internalDex = new File(context.getDir(INTERNAL_DEX_FOLDER, Context.MODE_PRIVATE), mSourceDex);
		if (!internalDex.exists()) {
			ensureDexIsPrepared(context, internalDex);
		}
		
		// Load the dex into internal cache
		DexFile dex;
		try {
			String internalDexPath = internalDex.getAbsolutePath();
			String internalCacheFolder = context.getDir(INTERNAL_CACHE_FOLDER, Context.MODE_PRIVATE).getAbsolutePath();
			String internalCachePath = generateOutputName(internalDex.getAbsolutePath(), internalCacheFolder);
			dex = DexFile.loadDex(internalDexPath, internalCachePath, 0);
		} catch (Exception e) {
			String.format("Unable to load '%s' to internal dex cache.", mSourceDex);
			throw new RuntimeException(e);
		}

		final ClassLoader classLoader = context.getClassLoader();
		final int sdkInt = Integer.parseInt(Build.VERSION.SDK);
		for (ConditionalLoad conditionalLoad : mConditionalLoads) {
			if (conditionalLoad.shouldLoadForApi(sdkInt)) {
				for (String className : conditionalLoad.getClasses()) {
					try {
						Log.i(TAG, String.format("Loading '%s' from '%s'", className, mSourceDex));
						dex.loadClass(className, classLoader);
					} catch (Exception e) {
						String.format("Unable to inject '%s'", className);
						throw new RuntimeException(e);
					}
				}
			}
		}
	}
	
	private void ensureDexIsPrepared(Context context, File internalDexFile) {
		BufferedInputStream dexIn = null;
		BufferedOutputStream dexOut = null;
		try {
			dexIn = new BufferedInputStream(context.getAssets().open(mSourceDex), BUFFER_SIZE);
			dexOut = new BufferedOutputStream(new FileOutputStream(internalDexFile), BUFFER_SIZE);
			byte[] buffer = new byte[BUFFER_SIZE];
			int len;
			while ((len = dexIn.read(buffer, 0, BUFFER_SIZE)) > 0) {
				dexOut.write(buffer, 0, len);
			}
		} catch (IOException e) {
			String error = String.format("Unable to load dex file '%s' from assets.", mSourceDex);
			throw new RuntimeException(error, e);
		} finally {
			if (dexIn != null) {
				try { dexIn.close(); } catch (Exception e) {}
			}
			if (dexOut != null) {
				try { dexOut.close(); } catch (Exception e) {}
			}
		}
	}
	
	private static String generateOutputName(String sourceFile, String outputDirectory) {
		final StringBuilder newStr = new StringBuilder(80);
		
		// Start with the output directory
		newStr.append(outputDirectory);
		if (!outputDirectory.endsWith("/")) {
			newStr.append("/");
		}

		// Get the filename component of the path
		String sourceFileName;
		final int lastSlash = sourceFile.lastIndexOf("/");
		if (lastSlash < 0) {
			sourceFileName = sourceFile;
		} else { 
			sourceFileName = sourceFile.substring(lastSlash + 1);
		}

		// Replace ".jar", ".zip", whatever with ".dex". We don't want to use
		// ".odex", because the build system uses that for files that are paired
		// with resource-only jar files. If the VM can assume that there's no
		// classes.dex in the matching jar, it doesn't need to open the jar to
		// check for updated dependencies, providing a slight performance boost
		// at startup. The use of ".dex" here matches the use on files in
		// /data/dalvik-cache.
		final int lastDot = sourceFileName.lastIndexOf(".");
		if (lastDot < 0) {
			newStr.append(sourceFileName);
		} else {
			newStr.append(sourceFileName, 0, lastDot);
		}
		newStr.append(".dex");

		return newStr.toString();
	}
}