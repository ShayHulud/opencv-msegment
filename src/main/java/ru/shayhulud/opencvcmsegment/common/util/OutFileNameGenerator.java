package ru.shayhulud.opencvcmsegment.common.util;

/**
 * Генератор имен файлов для аутпута.
 */
public final class OutFileNameGenerator {

	private static final String PNG = ".png";
	private static final String JPG = ".jpg";

	private OutFileNameGenerator() {
	}

	public static String generatePng(String filename, int step, String stepName) {
		return filename + "_" + String.format("%05d", step) + "_" + stepName + PNG;
	}

	public static String generateJpg(String filename, int step, String stepName) {
		return filename + "_" + String.format("%05d", step) + "_" + stepName + JPG;
	}
}
