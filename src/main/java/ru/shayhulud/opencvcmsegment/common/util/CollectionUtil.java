package ru.shayhulud.opencvcmsegment.common.util;

import java.util.List;

/**
 * Утиль для коллекций.
 */
public final class CollectionUtil {

	private CollectionUtil() {
	}

	public static <T> T getLastOf(List<T> list) {
		return list.isEmpty() ? null : list.get(list.size() - 1);
	}
}
