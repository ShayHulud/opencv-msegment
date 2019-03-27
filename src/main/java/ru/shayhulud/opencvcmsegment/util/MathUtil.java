package ru.shayhulud.opencvcmsegment.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class MathUtil {

	private MathUtil() {
	}

	public static Double normalize(Double v2n, Double fromMax, Double toMax) {
		Double result = (v2n / fromMax) * toMax;
		log.debug("normalizing {} from {} to {}: result = {}", v2n, fromMax, toMax, result);
		return result;
	}
}
