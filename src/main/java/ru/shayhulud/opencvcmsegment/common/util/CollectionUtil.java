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

	public static boolean booleanPrevPerMutation(boolean[] array, int first, int last) {
		if (first == last) {
			return false;
		}
		int i = last;
		if (first == --i) {
			return false;
		}

		while (true) {
			int i1, i2;
			i1 = i;
			if (i1 < --i) {
				i2 = last;
				while (!(--i2 < i)) {

				}
				boolean temp = array[i];
				array[i] = array[i2];
				array[i2] = temp;

				int r = (int) Math.floor((last - i1) / 2);
				for (int k = i1, l = 0; k <= i1 + r; k++, l++) {
					temp = array[k];
					array[k] = array[last - l];
					array[last - l] = temp;
				}
				return true;
			}
			if (i == first) {
				int r = (int) Math.floor(last / 2);
				for (int k = 0; k <= r; k++) {
					boolean temp = array[k];
					array[k] = array[last - k];
					array[last - k] = temp;
				}
				return false;
			}
		}
	}
}
