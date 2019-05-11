package ru.shayhulud.opencvcmsegment;

import nu.pattern.OpenCV;
import ru.shayhulud.opencvcmsegment.service.PictureService;

/**
 * Main для консольного запуска.
 */
public final class App {

	private App() {
	}

	public static void main(String[] args) {
		OpenCV.loadLocally();

		if (args.length != 3) {
			System.out.println("error parsing args");
			return;
		}

		for (int i = 0; i < args.length; i++) {
			System.out.println(String.format("arg %d: %s", i, args[i]));
		}

		PictureService pictureService = new PictureService();

		pictureService.colorAutoMarkerWatershed(args[0], args[1], args[2]);
		pictureService.shapeAutoMarkerWatershed(args[0], args[1], args[2]);

	}
}
