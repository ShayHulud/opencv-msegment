package ru.shayhulud.opencvcmsegment.util;

import lombok.extern.slf4j.Slf4j;

/**
 * Util for pixels methods.
 */
@Slf4j
public final class PixelUtil {
	public PixelUtil() {
	}

	public static boolean checkPixelRGB(byte[] pixel, int r, int g, int b) {
		if (pixel.length != 3) {
			log.error("Длинна массива RGB у пикселя не 3");
			return false;
		}
		return (pixel[0] == r) && (pixel[1] == g) && (pixel[2] == b);
	}

	public static void setPixelRGBValue(byte[] pixel, int r, int g, int b) {
		if (pixel.length != 3) {
			log.error("Длинна массива RGB у пикселя не 3");
			return;
		}
		pixel[0] = (byte) r;
		pixel[1] = (byte) g;
		pixel[2] = (byte) b;
	}
}
