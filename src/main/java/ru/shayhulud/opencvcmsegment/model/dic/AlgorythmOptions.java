package ru.shayhulud.opencvcmsegment.model.dic;

/**
 * Методы предобработки.
 */
public enum AlgorythmOptions {
	/**
	 * Медианный фильтр.
	 */
	MEDIAN_BLUR,
	/**
	 * Билатериальный
	 */
	BILATERIAL,
	/**
	 * Суперпиксели.
	 */
	SUPERPIXELS,
	/**
	 * Выборка диапазона из гистограммы.
	 */
	GISTO_DIAP,
	/**
	 * Нужен ч/б результат.
	 */
	BW_RESULT,
	/**
	 * multiotsu
	 */
	MULTI_OTSU
}
