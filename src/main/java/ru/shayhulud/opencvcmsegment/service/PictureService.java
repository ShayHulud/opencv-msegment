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
import org.opencv.core.Size;
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
//TODO: Сделать чтобы методы возвращали каждый свои маркеры, и отдельно метод, который применяет Watershed по маркерам.
@Slf4j
public class PictureService {

	//TODO: вынести в енум
	private static final String COLOR_METHOD = "_color";
	private static final String SHAPE_METHOD = "_shape";

	private static final String BLACK_BACKGROUND_OUT_NAME_SUFFIX = "_01_black_bg.png";
	private static final String LAPLASSIAN_SHARPER_OUT_NAME_SUFFIX = "_02_laplas_sharped.png";
	private static final String BLACK_WHITED_OUT_NAME_SUFFIX = "_03_bw.png";
	private static final String DISTANCE_TRANSFORMED_OUT_NAME_SUFFIX = "_04_distance.png";
	private static final String DISTANCE_PEAKS_OUT_NAME_SUFFIX = "_05_distance_peaks.png";
	private static final String MARKERS_OUT_NAME_SUFFIX = "_06_markers.png";
	private static final String RESULT_COLOR_OUT_NAME_SUFFIX = "_07_result.png";

	private static final String GRAYED_PICTURE_NAME_SUFFIX = "_01_grayed.png";
	private static final String BORDERS_NAME_SUFFIX = "_02_borders.png";
	private static final String GRAY_BORDERS_OUT_NAME_SUFFIX = "_03_gray_borders.png";
	private static final String MARKERS_SHAPE_OUT_NAME_SUFFIX = "_04_markers.png";
	private static final String RESULT_SHAPE_OUT_NAME_SUFFIX = "_05_result.png";

	private final Random rnd = new Random();


	public ImageInfo readPicture(String picturePath, String outMainFolder, String pictureName) throws IOException {

		ImageInfo ii = new ImageInfo();

		String pictureFullPath = picturePath + File.separator + pictureName;
		log.info("pictureFullPath = {}", pictureFullPath);

		//load image content to temp file.
		File pictureFile = new File(pictureFullPath);
		log.info("read picture size of: {}", pictureFile.length());

		ii.setImageFileName(pictureName);

		//Convert file to openCV Mat
		Mat src = Imgcodecs.imread(pictureFile.getCanonicalPath());
		if (src.dataAddr() == 0) {
			throw new IOException();
		}
		ii.setMat(src);

		String outDirName = createOutputFolder(picturePath, outMainFolder);
		ii.setOutputDirName(outDirName);
		return ii;
	}

	private String createOutputFolder(String picturePath, String outMainFolder) throws IOException {
		String folderName = DateUtils.fromUnixFormatted(new Date().getTime());
		File outputFolder = new File(
			picturePath + File.separator +
				outMainFolder + File.separator +
				folderName
		);
		String dp = outputFolder.getCanonicalPath();
		if (!outputFolder.isDirectory()) {
			boolean created = outputFolder.mkdirs();
			if (created) {
				log.info("out directory {} is created", dp);
			}
		} else {
			log.info("out directory {} already created", dp);
		}
		return dp;
	}

	private void showImage(Mat out, ImageInfo ii, String method, String suffix) throws IOException {
		File result = new File(
			ii.getOutputDirName() + File.separator +
				ii.getImageFileName() + method + suffix
		);
		String outputPath = result.getCanonicalPath();
		Imgcodecs.imwrite(outputPath, out);
		log.info("wrote image {}", outputPath);
	}

	private void showImage(Mat out, ImageInfo ii, String method, String suffix, double multiplier) throws IOException {
		Mat multipliedOut = out.clone();
		Core.multiply(multipliedOut, new Scalar(multiplier), multipliedOut);
		showImage(multipliedOut, ii, method, suffix);
	}

	private byte[] generateBGRColor() {
		byte b = (byte) this.rnd.nextInt(256);
		byte g = (byte) this.rnd.nextInt(256);
		byte r = (byte) this.rnd.nextInt(256);
		return new byte[]{b, g, r};
	}

	public void colorAutoMarkerWatershed(String picturePath, String outMainFolder, String pictureName) {
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
			showImage(src, ii, COLOR_METHOD, BLACK_BACKGROUND_OUT_NAME_SUFFIX);

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
			showImage(imgResult, ii, COLOR_METHOD, LAPLASSIAN_SHARPER_OUT_NAME_SUFFIX);

			Mat bw = new Mat();
			Imgproc.cvtColor(src, bw, Imgproc.COLOR_BGR2GRAY);
			Imgproc.threshold(bw, bw, 40, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
			showImage(bw, ii, COLOR_METHOD, BLACK_WHITED_OUT_NAME_SUFFIX);

			Mat distance = new Mat();
			Imgproc.distanceTransform(bw, distance, Imgproc.CV_DIST_L2, 5);
			Core.normalize(distance, distance, 0, 1., Core.NORM_MINMAX);
			showImage(distance, ii, COLOR_METHOD, DISTANCE_TRANSFORMED_OUT_NAME_SUFFIX, 1000);

			Imgproc.threshold(distance, distance, .4, 1., Imgproc.THRESH_BINARY);
			Mat kernel1 = Mat.ones(3, 3, CvType.CV_8UC1);
			Imgproc.dilate(distance, distance, kernel1);
			showImage(distance, ii, COLOR_METHOD, DISTANCE_PEAKS_OUT_NAME_SUFFIX, 1000);

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
			showImage(markers, ii, COLOR_METHOD, MARKERS_OUT_NAME_SUFFIX, 10000d);

			Imgproc.watershed(src, markers);

			Mat mark = Mat.zeros(markers.size(), CvType.CV_8UC1);
			markers.convertTo(mark, CvType.CV_8UC1);
			Core.bitwise_not(mark, mark);

			List<byte[]> colors = new ArrayList<>();
			for (int i = 0; i < contours.size(); i++) {
				colors.add(generateBGRColor());
			}

			//define color for background
			byte[] backroundColor = generateBGRColor();

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
			showImage(dst, ii, COLOR_METHOD, RESULT_COLOR_OUT_NAME_SUFFIX);

		} catch (IOException e) {
			log.error("There is an error with file stream processing", e);
		}
	}

	public void shapeAutoMarkerWatershed(String picturePath, String outMainFolder, String pictureName) {
		try {
			ImageInfo ii = readPicture(picturePath, outMainFolder, pictureName);
			Mat src = ii.getMat().clone();


			Mat markerMask = new Mat();
			Mat srcGray = new Mat();
			Imgproc.cvtColor(src, markerMask, Imgproc.COLOR_BGR2GRAY);
			Imgproc.cvtColor(markerMask, srcGray, Imgproc.COLOR_GRAY2BGR);
			markerMask = Mat.zeros(markerMask.size(), markerMask.type());
			showImage(srcGray, ii, SHAPE_METHOD, GRAYED_PICTURE_NAME_SUFFIX);

			//TODO: lowTreshold need to be various;
			int lowTreshold = 5;
			int ratio = 10;
			Mat brdGray = new Mat();
			Imgproc.cvtColor(src, brdGray, Imgproc.COLOR_BGR2GRAY);
			Imgproc.blur(brdGray, brdGray, new Size(5, 5));
			Imgproc.Canny(brdGray, brdGray, lowTreshold, lowTreshold * ratio);
			Mat brdDst = Mat.zeros(src.size(), src.type());
			src.copyTo(brdDst, brdGray);
			showImage(brdDst, ii, SHAPE_METHOD, BORDERS_NAME_SUFFIX);
			showImage(brdGray, ii, SHAPE_METHOD, GRAY_BORDERS_OUT_NAME_SUFFIX);
			brdGray.copyTo(markerMask);

			//Removing small parts;
			//vertical
			Mat vKernel = new Mat(3, 3, markerMask.type()) {{
				put(0, 0, 0);
				put(0, 1, 1);
				put(0, 2, 0);

				put(1, 0, 0);
				put(1, 1, 1);
				put(1, 2, 0);

				put(2, 0, 0);
				put(2, 1, 1);
				put(2, 2, 0);
			}};
			Mat mmV = markerMask.clone();
			Imgproc.erode(mmV, mmV, vKernel);
			Imgproc.dilate(mmV, mmV, vKernel);
			showImage(mmV, ii, SHAPE_METHOD, "_remove_smaler_V.png");
			//horisontal
			Mat hKernel = new Mat(3, 3, markerMask.type()) {{
				put(0, 0, 0);
				put(0, 1, 0);
				put(0, 2, 0);

				put(1, 0, 1);
				put(1, 1, 1);
				put(1, 2, 1);

				put(2, 0, 0);
				put(2, 1, 0);
				put(2, 2, 0);
			}};
			Mat mmH = markerMask.clone();
			Imgproc.erode(mmH, mmH, hKernel);
			Imgproc.dilate(mmH, mmH, hKernel);
			showImage(mmH, ii, SHAPE_METHOD, "_remove_smaler_H.png");
			//summ
			Core.add(mmH, mmV, mmH);
			showImage(mmH, ii, SHAPE_METHOD, "_V+H_smaler.png");

			Mat median = markerMask.clone();
			Imgproc.medianBlur(median, median, 3);
			showImage(median, ii, SHAPE_METHOD, "_median_remove_smaler.png");
			Core.add(median, mmH, median);
			showImage(median, ii, SHAPE_METHOD, "_V+H+M_remove_smaler.png");

			List<MatOfPoint> contours = new ArrayList<>();
			MatOfInt4 hierarchy = new MatOfInt4();

			Imgproc.findContours(markerMask, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
			if (contours.isEmpty()) {
				log.info("contours is empty");
				return;
			}

			Mat markers = Mat.zeros(markerMask.size(), CvType.CV_32S);
			for (int i = 0; i < contours.size(); i++) {
				Imgproc.drawContours(markers, contours, i, Scalar.all(i + 1), 0, 8, hierarchy, Integer.MAX_VALUE, new Point());
			}
			//markerMask.convertTo(markers, CvType.CV_32S);
			Imgproc.circle(markers, new Point(5, 5), 3, new Scalar(255, 255, 255), -1);

			showImage(markers, ii, SHAPE_METHOD, MARKERS_SHAPE_OUT_NAME_SUFFIX, 10000);

			List<byte[]> colors = new ArrayList<>();
			for (int i = 0; i < contours.size(); i++) {
				colors.add(generateBGRColor());
			}
			byte[] backroundColor = generateBGRColor();

			Imgproc.watershed(src, markers);

			Mat wshed = new Mat(markers.size(), CvType.CV_8UC3);
			for (int i = 0; i < markers.rows(); i++) {
				for (int j = 0; j < markers.cols(); j++) {
					int index = (int) markers.get(i, j)[0];
					if (index > 0 && index <= contours.size()) {
						wshed.put(i, j, colors.get(index - 1));
					} else {
						wshed.put(i, j, backroundColor);
					}
				}
			}
			showImage(wshed, ii, SHAPE_METHOD, RESULT_SHAPE_OUT_NAME_SUFFIX);

		} catch (IOException e) {
			log.error("There is an error with file stream processing", e);
		}
	}
}
