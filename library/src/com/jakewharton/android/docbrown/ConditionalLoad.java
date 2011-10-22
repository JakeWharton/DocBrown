package com.jakewharton.android.docbrown;

import java.util.Set;
import android.content.Context;

/**
 * Represents a set of classes and an API level condition to determine on which
 * levels the load should be attempted.
 */
public final class ConditionalLoad {
	/** Creating {@link DocBrown} instance. */
	private final DocBrown mParent;
	/** Set of fully-qualified class names associated with this condition. */
	private final Set<String> mClasses;
	/** Lowest API level (inclusive) on which to load. */
	private Integer mLowestApiLevel;
	/** Highest API level (inclusive) on which to load. */
	private Integer mHighestApiLevel;
	
	
	/**
	 * Create a new instance.
	 * 
	 * @param parent Parent {@link DocBrown} instance.
	 * @param classes Set of class names.
	 */
	/*package*/ ConditionalLoad(DocBrown parent, Set<String> classes) {
		mParent = parent;
		mClasses = classes;
	}
	
	
	/**
	 * The class should only be loaded on API levels which are less than the
	 * specified level.
	 * 
	 * @param apiLevel Highest API level (exclusive).
	 * @return The creating {@link DocBrown} instance.
	 */
	public DocBrown before(int apiLevel) {
		mLowestApiLevel = null;
		mHighestApiLevel = apiLevel - 1;
		return mParent;
	}
	
	/**
	 * The class should only be loaded on API levels which are greater than the
	 * specified level.
	 * 
	 * @param apiLevel Lowest API level (exclusive).
	 * @return The creating {@link DocBrown} instance.
	 */
	public DocBrown after(int apiLevel) {
		mLowestApiLevel = apiLevel + 1;
		mHighestApiLevel = null;
		return mParent;
	}
	
	/**
	 * The class should only be loaded on API levels between the specified
	 * bounds.
	 * 
	 * @param lowestApiLevel Lowest API level (inclusive).
	 * @param highestApiLevel Highest API level (inclusive).
	 * @return The creating {@link DocBrown} instance.
	 */
	public DocBrown between(int lowestApiLevel, int highestApiLevel) {
		mLowestApiLevel = lowestApiLevel;
		mHighestApiLevel = highestApiLevel;
		return mParent;
	}
	
	/**
	 * Shortcut back to the {@link DocBrown} instance if the class should be
	 * loaded on all API levels.
	 * 
	 * @see DocBrown#load(String...)
	 */
	public ConditionalLoad load(String... classNames) {
		return mParent.load(classNames);
	}
	
	/**
	 * Shortcut back to the {@link DocBrown} instance if the class should be
	 * loaded on all API levels.
	 * 
	 * @see DocBrown#into(Context)
	 */
	public void into(Context context) {
		mParent.into(context);
	}
	
	/**
	 * Determine whether or not this load applies to the specified API level.
	 * 
	 * @param apiLevel Running API level.
	 * @return True if the class should be loaded.
	 */
	public boolean shouldLoadForApi(int apiLevel) {
		if ((mLowestApiLevel == null) && (mHighestApiLevel == null)) {
			return true;
		}
		if (mLowestApiLevel == null) {
			return apiLevel <= mHighestApiLevel; 
		}
		if (mHighestApiLevel == null) {
			return apiLevel >= mLowestApiLevel;
		}
		return (mLowestApiLevel <= apiLevel) && (apiLevel <= mHighestApiLevel);
	}
	
	/**
	 * Get the list of all classes specified for this loader.
	 * 
	 * @return Set of fully qualified class names.
	 */
	public Set<String> getClasses() {
		return mClasses;
	}
}