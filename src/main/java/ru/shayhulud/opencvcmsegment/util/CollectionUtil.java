package ru.shayhulud.opencvcmsegment.util;

import java.util.List;

public final class CollectionUtil {

	private CollectionUtil() {
	}

	public static <T> T getLastOf(List<T> list) {
		return list.isEmpty() ? null : list.get(list.size() - 1);
	}
}
