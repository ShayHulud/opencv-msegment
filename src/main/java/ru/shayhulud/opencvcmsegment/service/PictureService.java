package ru.shayhulud.opencvcmsegment.service;

import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import ru.shayhulud.opencvcmsegment.util.PixelUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Picture processing service.
 */
@Slf4j
public class PictureService {

	public void readPicture(String picturePath, String pictureName) {

		File tempFile = null;

		try {
			//load image content to temp file.
			tempFile = new File(picturePath + "\\" + pictureName);
			log.info("filled file size:{}", tempFile.length());

			//Convert file to openCV Mat
			//TODO:check which path is needed
			Mat src = Imgcodecs.imread(tempFile.getCanonicalPath());
			if (src.dataAddr() == 0) {
				throw new IOException();
			}
			log.info("src type {}", src.type());

			for (int x = 0; x < src.rows(); x++) {
				for (int y = 0; y < src.cols(); y++) {
					byte[] vec3b = new byte[3];
					src.get(x, y, vec3b);
					if (PixelUtil.checkPixelRGB(vec3b, 255, 255, 255)) {
						PixelUtil.setPixelRGBValue(vec3b, 0, 0, 0);
						src.put(x, y, vec3b);
					}
				}
			}
			//TODO:show blackfoned
			Mat kernel = new MatOfFloat(1f, 1f, 1f, 1f, -8f, 1f, 1f, 1f, 1f);
			//possibly need to clone from src or zeros of src;
			Mat imgLaplasian = new Mat();
			Mat sharp = src.clone();
			Imgproc.filter2D(sharp, imgLaplasian, CvType.CV_32F, kernel);
			src.convertTo(sharp, CvType.CV_32F);
			Mat imgResult = new Mat();
			Core.subtract(sharp, imgLaplasian, imgResult);
			imgResult.convertTo(imgResult, CvType.CV_8UC3);
			imgLaplasian.convertTo(imgLaplasian, CvType.CV_8UC3);
			//TODO:show sharped image - laplassianned
			imgResult.copyTo(src);
			Mat bw = new Mat();
			Imgproc.cvtColor(src, bw, Imgproc.COLOR_BGR2GRAY);
			Imgproc.threshold(bw, bw, 40, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
			//TODO:show bw image
			Mat distance = new Mat();
			Imgproc.distanceTransform(bw, distance, Imgproc.CV_DIST_L2, 3);
			Core.normalize(distance, distance, 0, 1, Core.NORM_MINMAX);
			Imgproc.threshold(distance, distance, 0.4, 1, Imgproc.THRESH_BINARY);
			Mat kernel1 = Mat.ones(3, 3, CvType.CV_8UC1);
			Imgproc.dilate(distance, distance, kernel1);
			//TODO: show distance transform
			Mat dist_8u = new Mat();
			distance.convertTo(dist_8u, CvType.CV_8U);
			List<MatOfPoint> contours = new ArrayList<>();
			//0 to CV_RETR_EXTERNAL, 2 to CV_CHAIN_APPROX_SIMPLE
			Imgproc.findContours(dist_8u, contours, new MatOfInt4(), 0, 2);
			Mat markers = Mat.zeros(distance.size(), CvType.CV_32SC1);
			for (int i = 0; i < contours.size(); i++) {
				Imgproc.drawContours(markers, contours, i, Scalar.all(i + 1), -1);
			}
			Imgproc.circle(markers, new Point(5, 5), 3, new Scalar(255, 255, 255), -1);
			//TODO:Show markers with multiply on 10000
			Imgproc.watershed(src, markers);
			Mat mark = Mat.zeros(markers.size(), CvType.CV_8UC1);
			markers.convertTo(mark, CvType.CV_8UC1);
			Core.bitwise_not(mark, mark);
			//TODO: show mark image

			log.info("markers type {}", markers.type());

			List<byte[]> colors = new ArrayList<>();
			Random rnd = new Random();
			for (int i = 0; i < contours.size(); i++) {
				byte b = (byte) rnd.nextInt(256);
				byte g = (byte) rnd.nextInt(256);
				byte r = (byte) rnd.nextInt(256);
				colors.add(new byte[]{b, g, r});
			}

			Mat dst = Mat.zeros(markers.size(), CvType.CV_8UC3);
			for (int i = 0; i < markers.rows(); i++) {
				for (int j = 0; j < markers.cols(); j++) {
					byte[] buff = new byte[(int) markers.depth() * markers.channels()];
					int index = markers.get(i, j, buff);
					if (index > 0 && index <= contours.size()) {
						byte[] vec3b = new byte[3];
						PixelUtil.setPixelRGBValue(
							vec3b,
							colors.get(index - i)[0],
							colors.get(index - i)[1],
							colors.get(index - i)[2]
						);
						dst.put(i, j, vec3b);
					} else {
						dst.put(i, j, new byte[]{0, 0, 0});
					}
				}
			}

			File result = new File(picturePath + "\\" + "dst.jpg");
			Imgcodecs.imwrite(result.getCanonicalPath(), dst);

			//TODO:refactor to multiple methods, for ability to show image on different stages.


		} catch (IOException e) {
			log.error("There is an error with file stream processing", e);
		}
	}
}
