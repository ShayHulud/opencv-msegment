package ru.shayhulud.opencvcmsegment;

import nu.pattern.OpenCV;
import ru.shayhulud.opencvcmsegment.service.PictureService;

public class App {

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

		pictureService.amw(args[0], args[1], args[2]);
	}
}
