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
import ru.shayhulud.opencvcmsegment.model.ImageInfo;
import ru.shayhulud.opencvcmsegment.util.DateUtils;
import ru.shayhulud.opencvcmsegment.util.PixelUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Picture processing service.
 */
@Slf4j
public class PictureService {

	private static final String BLACK_BACKGROUND_OUT_NAME_SUFFIX = "_01_black_bg.png";
	private static final String LAPLASSIAN_SHARPER_OUT_NAME_SUFFIX = "_02_laplas_sharped.png";
	private static final String BLACK_WHITED_OUT_NAME_SUFFIX = "_03_bw.png";
	private static final String DISTANCE_TRANSFORMED_OUT_NAME_SUFFIX = "_04_distance.png";
	private static final String DISTANCE_PEAKS_OUT_NAME_SUFFIX = "_05_distance_peaks.png";
	private static final String MARKERS_OUT_NAME_SUFFIX = "_06_markers.png";
	private static final String RESULT_OUT_NAME_SUFFIX = "_07_result.png";


	public ImageInfo readPicture(String picturePath, String outMainFolder, String pictureName) throws IOException {

		ImageInfo ii = new ImageInfo();

		String pictureFullPath = picturePath + File.separator + pictureName;
		log.info("pictureFullPath = {}", pictureFullPath);

		//load image content to temp file.
		File pictureFile = new File(pictureFullPath);
		log.info("read picture size of:{}", pictureFile.length());

		ii.setImageFileName(pictureName);

		//Convert file to openCV Mat
		Mat src = Imgcodecs.imread(pictureFile.getCanonicalPath());
		if (src.dataAddr() == 0) {
			throw new IOException();
		}
		ii.setMat(src);

		String outDname = createOutputFolder(picturePath, outMainFolder);
		ii.setOutputDirName(outDname);
		return ii;
	}

	private String createOutputFolder(String picturePath, String outMainFolder) throws IOException {
		String folderName = DateUtils.fromUnixFormatted(new Date().getTime());
		File outputFolder = new File(
			picturePath + File.separator +
				outMainFolder + File.separator +
				folderName
		);
		boolean created = outputFolder.mkdirs();
		String dp = outputFolder.getCanonicalPath();
		if (created) {
			log.info("out directory {} is created", dp);
		}
		return dp;
	}

	private void showImage(Mat out, ImageInfo ii, String suffix) throws IOException {
		File result = new File(
			ii.getOutputDirName() + File.separator +
				ii.getImageFileName() + suffix
		);
		String outputPath = result.getCanonicalPath();
		Imgcodecs.imwrite(outputPath, out);
		log.info("wrote image {}", outputPath);
	}

	private void showImage(Mat out, ImageInfo ii, String suffix, double multiplier) throws IOException {
		Mat multipliedOut = out.clone();
		Core.multiply(multipliedOut, new Scalar(multiplier), multipliedOut);
		showImage(multipliedOut, ii, suffix);
	}

	public void amw(String picturePath, String outMainFolder, String pictureName) {

		try {

			ImageInfo ii = readPicture(picturePath, outMainFolder, pictureName);
			Mat src = ii.getMat().clone();

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
			showImage(src, ii, BLACK_BACKGROUND_OUT_NAME_SUFFIX);

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
			imgResult.copyTo(src);
			showImage(imgResult, ii, LAPLASSIAN_SHARPER_OUT_NAME_SUFFIX);

			Mat bw = new Mat();
			Imgproc.cvtColor(src, bw, Imgproc.COLOR_BGR2GRAY);
			Imgproc.threshold(bw, bw, 40, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
			showImage(bw, ii, BLACK_WHITED_OUT_NAME_SUFFIX);

			Mat distance = new Mat();
			Imgproc.distanceTransform(bw, distance, Imgproc.CV_DIST_L2, 5);
			Core.normalize(distance, distance, 0, 1., Core.NORM_MINMAX);
			showImage(distance, ii, DISTANCE_TRANSFORMED_OUT_NAME_SUFFIX, 1000);

			Imgproc.threshold(distance, distance, .4, 1., Imgproc.THRESH_BINARY);
			Mat kernel1 = Mat.ones(3, 3, CvType.CV_8UC1);
			Imgproc.dilate(distance, distance, kernel1);
			showImage(distance, ii, DISTANCE_PEAKS_OUT_NAME_SUFFIX, 1000);

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
			//Output markers * 10000
			showImage(markers, ii, MARKERS_OUT_NAME_SUFFIX, 10000d);

			Imgproc.watershed(src, markers);

			Mat mark = Mat.zeros(markers.size(), CvType.CV_8UC1);
			markers.convertTo(mark, CvType.CV_8UC1);
			Core.bitwise_not(mark, mark);

			List<byte[]> colors = new ArrayList<>();
			Random rnd = new Random();
			for (int i = 0; i < contours.size(); i++) {
				byte b = (byte) rnd.nextInt(256);
				byte g = (byte) rnd.nextInt(256);
				byte r = (byte) rnd.nextInt(256);
				colors.add(new byte[]{b, g, r});
			}

			//define color for background
			byte[] backroundColor = new byte[3];
			backroundColor[0] = (byte) rnd.nextInt(256);
			backroundColor[1] = (byte) rnd.nextInt(256);
			backroundColor[2] = (byte) rnd.nextInt(256);

			Mat dst = Mat.zeros(markers.size(), CvType.CV_8UC3);
			for (int i = 0; i < markers.rows(); i++) {
				for (int j = 0; j < markers.cols(); j++) {
					double[] buff = markers.get(i, j);
					int index = (int) buff[0];
					if (index > 0 && index <= contours.size()) {
						byte[] vec3b = new byte[3];
						PixelUtil.setPixelRGBValue(
							vec3b,
							colors.get(index - 1)[0],
							colors.get(index - 1)[1],
							colors.get(index - 1)[2]
						);
						dst.put(i, j, vec3b);
					} else {
						dst.put(i, j, backroundColor);
					}
				}
			}
			showImage(dst, ii, RESULT_OUT_NAME_SUFFIX);

		} catch (IOException e) {
			log.error("There is an error with file stream processing", e);
		}
	}
}
