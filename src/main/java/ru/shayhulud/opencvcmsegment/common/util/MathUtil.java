package ru.shayhulud.opencvcmsegment.common.util;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Утиль для математических операций.
 */
@Slf4j
public final class MathUtil {

	private MathUtil() {
	}

	public static Double normalize(Double v2n, Double fromMax, Double toMax) {
		Double result = (v2n / fromMax) * toMax;
		log.debug("normalizing {} from {} to {}: result = {}", v2n, fromMax, toMax, result);
		return result;
	}

	public static Integer meanI(List<? extends Number> values) {
		return meanD(values).intValue();
	}

	public static Double meanD(List<? extends Number> values) {
		double mean = 0D;
		for (Number n : values) {
			mean += n.doubleValue();
		}
		return mean / values.size();
	}
}
