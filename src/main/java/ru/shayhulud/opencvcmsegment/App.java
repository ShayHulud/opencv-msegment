package ru.shayhulud.opencvcmsegment;

import nu.pattern.OpenCV;
import ru.shayhulud.opencvcmsegment.service.PictureService;

public class App {

	public static void main(String[] args) {
		OpenCV.loadLocally();

		PictureService pictureService = new PictureService();

		pictureService.readPicture("D:\\git\\opencv-msegment\\src\\main\\resources", "hkp.jpg");
	}
}
