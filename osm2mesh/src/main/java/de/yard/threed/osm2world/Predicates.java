package de.yard.threed.osm2world;

import com.google.common.base.Predicate;

public final class Predicates {
	
	/** prevents instantiation */
	private Predicates() { }
	
	/**
	 * returns a predicate that is true for objects that are instance of a type
	 */
	public static final Predicate<Object> hasType(final Class<?> type) {
		
		return new Predicate<Object>() {			
			@Override
			public boolean apply(Object o) {
				return type.isInstance(o);
			}
		};
		
	}

}
