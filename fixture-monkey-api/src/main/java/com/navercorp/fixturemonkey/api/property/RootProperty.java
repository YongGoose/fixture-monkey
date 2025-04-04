package com.navercorp.fixturemonkey.api.property;

import java.lang.reflect.AnnotatedType;

import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

/**
 * It is a property for a root type.
 * It does not support the equivalence of the type.
 */
@API(since = "0.4.0", status = Status.MAINTAINED)
public final class RootProperty implements TreeRootProperty {
	private final Property delgeatedProperty;

	/**
	 * It is deprecated. Use {@link #RootProperty(Property)} instead.
	 */
	@Deprecated
	public RootProperty(AnnotatedType annotatedType) {
		this.delgeatedProperty = new TypeParameterProperty(annotatedType);
	}

	public RootProperty(Property delgeatedProperty) {
		this.delgeatedProperty = delgeatedProperty;
	}

	@Override
	public Property getDelgatedProperty() {
		return delgeatedProperty;
	}

	@Override
	public String toString() {
		return "RootProperty{"
			+ "annotatedType=" + delgeatedProperty.getAnnotatedType() + '}';
	}
}
