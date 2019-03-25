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
import ru.shayhulud.opencvcmsegment.exceptions.IncorrectMatBodyLengthException;
import ru.shayhulud.opencvcmsegment.model.ImageInfo;
import ru.shayhulud.opencvcmsegment.util.DateUtils;
import ru.shayhulud.opencvcmsegment.util.OutFileNameGenerator;
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

	private void showImage(Mat out, ImageInfo ii, String method, int step, String stepName) throws IOException {
		String fileName = OutFileNameGenerator.generatePng(method + ii.getImageFileName(), step, stepName);
		File result = new File(ii.getOutputDirName() + File.separator + fileName);
		String outputPath = result.getCanonicalPath();
		Imgcodecs.imwrite(outputPath, out);
		log.info("wrote image {}", outputPath);
	}

	private void showImage(Mat out, ImageInfo ii, String method, int step, String stepName, double multiplier)
		throws IOException {

		Mat multipliedOut = out.clone();
		Core.multiply(multipliedOut, new Scalar(multiplier), multipliedOut);
		showImage(multipliedOut, ii, method, step, stepName);
	}

	private byte[] generateBGRColor() {
		byte b = (byte) this.rnd.nextInt(256);
		byte g = (byte) this.rnd.nextInt(256);
		byte r = (byte) this.rnd.nextInt(256);
		return new byte[]{b, g, r};
	}

	public void colorAutoMarkerWatershed(String picturePath, String outMainFolder, String pictureName) {
		try {
			int step = 0;
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
			showImage(src, ii, COLOR_METHOD, ++step, "black_bg");

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
			showImage(imgResult, ii, COLOR_METHOD, ++step, "laplassian_sharp");

			Mat bw = new Mat();
			Imgproc.cvtColor(src, bw, Imgproc.COLOR_BGR2GRAY);
			Imgproc.threshold(bw, bw, 40, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
			showImage(bw, ii, COLOR_METHOD, ++step, "bw");

			Mat distance = new Mat();
			Imgproc.distanceTransform(bw, distance, Imgproc.CV_DIST_L2, 5);
			Core.normalize(distance, distance, 0, 1., Core.NORM_MINMAX);
			showImage(distance, ii, COLOR_METHOD, ++step, "distance_transform", 1000);

			Imgproc.threshold(distance, distance, .4, 1., Imgproc.THRESH_BINARY);
			Mat kernel1 = Mat.ones(3, 3, CvType.CV_8UC1);
			Imgproc.dilate(distance, distance, kernel1);
			showImage(distance, ii, COLOR_METHOD, ++step, "distance_peaks", 1000);

			Mat dist_8u = new Mat();
			distance.convertTo(dist_8u, CvType.CV_8U);
			List<MatOfPoint> contours = new ArrayList<>();
			MatOfInt4 hierarchy = new MatOfInt4();
			//https://docs.opencv.org/2.4/modules/imgproc/doc/structural_analysis_and_shape_descriptors.html?highlight=findcontours#findcontours
			Imgproc.findContours(dist_8u, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_NONE);
			Mat markers = Mat.zeros(distance.size(), CvType.CV_32SC1);
			for (int i = 0; i < contours.size(); i++) {
				Imgproc.drawContours(markers, contours, i, Scalar.all(i + 1), -1, 8, hierarchy, Integer.MAX_VALUE, new Point());
			}
			Imgproc.circle(markers, new Point(5, 5), 3, new Scalar(255, 255, 255), -1);
			//Output markers * 10000
			showImage(markers, ii, COLOR_METHOD, ++step, "markers", 10000d);

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
			showImage(dst, ii, COLOR_METHOD, ++step, "result");

		} catch (IOException e) {
			log.error("There is an error with file stream processing", e);
		}
	}

	public void shapeAutoMarkerWatershed(String picturePath, String outMainFolder, String pictureName) {
		try {
			int step = 0;
			ImageInfo ii = readPicture(picturePath, outMainFolder, pictureName);
			Mat src = ii.getMat().clone();


			Mat markerMask = new Mat();
			Mat srcGray = new Mat();
			Imgproc.cvtColor(src, markerMask, Imgproc.COLOR_BGR2GRAY);
			Imgproc.cvtColor(markerMask, srcGray, Imgproc.COLOR_GRAY2BGR);
			markerMask = Mat.zeros(markerMask.size(), markerMask.type());
			showImage(srcGray, ii, SHAPE_METHOD, ++step, "grayed");

			//TODO: lowTreshold need to be various;
			int lowTreshold = 5;
			int ratio = 10;
			Mat brdGray = src.clone();
			Imgproc.cvtColor(src, brdGray, Imgproc.COLOR_BGR2GRAY);
//			Imgproc.blur(brdGray, brdGray, new Size(5, 5));
			Imgproc.Canny(brdGray, brdGray, lowTreshold, lowTreshold * ratio);
			Mat brdDst = Mat.zeros(src.size(), src.type());
			src.copyTo(brdDst, brdGray);
			showImage(brdDst, ii, SHAPE_METHOD, ++step, "borders");
			showImage(brdGray, ii, SHAPE_METHOD, ++step, "gray_borders");
			brdGray.copyTo(markerMask);

			//TODO: вынести в методы.
			//Removing small parts;
			//vertical
			Mat vKernel = this.create3x3Kernel(markerMask.type(), new int[]{
				0, 1, 0,
				0, 1, 0,
				0, 1, 0});
			Mat mmV = markerMask.clone();
			Imgproc.erode(mmV, mmV, vKernel);
			Imgproc.dilate(mmV, mmV, vKernel);
			//horisontal
			Mat hKernel = this.create3x3Kernel(markerMask.type(), new int[]{
				0, 0, 0,
				1, 1, 1,
				0, 0, 0});
			Mat mmH = markerMask.clone();
			Imgproc.erode(mmH, mmH, hKernel);
			Imgproc.dilate(mmH, mmH, hKernel);
			//LVH
			Mat lvhKernel = this.create3x3Kernel(markerMask.type(), new int[]{
				1, 0, 0,
				0, 1, 0,
				0, 0, 1});
			Mat mmLVH = markerMask.clone();
			Imgproc.erode(mmLVH, mmLVH, lvhKernel);
			Imgproc.dilate(mmLVH, mmLVH, lvhKernel);
//			showImage(mmLVH, ii, SHAPE_METHOD, "_remove_smaler_LVH.png");
			Mat rvhKernel = this.create3x3Kernel(markerMask.type(), new int[]{
				0, 0, 1,
				0, 1, 0,
				1, 0, 0});
			Mat mmRVH = markerMask.clone();
			Imgproc.erode(mmRVH, mmRVH, rvhKernel);
			Imgproc.dilate(mmRVH, mmRVH, rvhKernel);
//			showImage(mmRVH, ii, SHAPE_METHOD, "_remove_smaler_RVH.png");
			//summ
			Core.add(mmH, mmV, mmH);
//			showImage(mmH, ii, SHAPE_METHOD, "_V+H_smaler.png");
			Core.add(mmLVH, mmRVH, mmLVH);
//			showImage(mmLVH, ii, SHAPE_METHOD, "_LVH+RVH_smaler.png");
			Core.add(mmH, mmLVH, mmH);
			showImage(mmH, ii, SHAPE_METHOD, ++step, "ALL_smaler.png");

			Imgproc.dilate(mmH, mmH, Mat.ones(5, 5, markerMask.type()));
			Imgproc.erode(mmH, mmH, Mat.ones(5, 5, markerMask.type()));
			showImage(mmH, ii, SHAPE_METHOD, ++step, "open_smaler.png");

			mmH.copyTo(markerMask);

//			Mat median = markerMask.clone();
//			Imgproc.medianBlur(mmH, mmH, 3);
//			showImage(mmH, ii, SHAPE_METHOD, "_median_remove_smaler.png");
//			Core.add(median, mmH, median);
//			showImage(median, ii, SHAPE_METHOD, "_V+H+M_remove_smaler.png");

			List<MatOfPoint> contours = new ArrayList<>();
			MatOfInt4 hierarchy = new MatOfInt4();

			Imgproc.findContours(markerMask, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_NONE);
			if (contours.isEmpty()) {
				log.info("contours is empty");
				return;
			}

			Mat markers = Mat.zeros(markerMask.size(), CvType.CV_32S);
			for (int i = 0; i < contours.size(); i++) {
				Imgproc.drawContours(markers, contours, i, Scalar.all(i + 1), 3, 8, hierarchy, Integer.MAX_VALUE, new Point());
			}
			//TODO:Добавить еще одну итерацию с выделением контуров.
			//markerMask.convertTo(markers, CvType.CV_32S);
			Imgproc.circle(markers, new Point(5, 5), 3, new Scalar(255, 255, 255), -1);

			showImage(markers, ii, SHAPE_METHOD, ++step, "markers", 10000);

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
			showImage(wshed, ii, SHAPE_METHOD, ++step, "result");

		} catch (IOException e) {
			log.error("There is an error with file stream processing", e);
		}
	}

	private Mat create3x3Kernel(int type, int[] kernelBody) {
		if (kernelBody.length != 9) {
			throw new IncorrectMatBodyLengthException("wrong kernel length");
		}

//		Mat kernel = new Mat(3, 3, type) {{
//			put(0, 0, kernelBody[0]);
//			put(0, 1, kernelBody[1]);
//			put(0, 2, kernelBody[2]);
//
//			put(1, 0, kernelBody[3]);
//			put(1, 1, kernelBody[4]);
//			put(1, 2, kernelBody[5]);
//
//			put(2, 0, kernelBody[6]);
//			put(2, 1, kernelBody[7]);
//			put(2, 2, kernelBody[8]);
//		}};

		double[] kernelBodyD = new double[kernelBody.length];
		for (int i = 0; i < kernelBody.length; i++) {
			kernelBodyD[i] = (double) kernelBody[i];
		}

		Mat kernel = new Mat(3, 3, type, new Scalar(kernelBodyD));
		return kernel;

		//TODO: check put(0,0,allkernelbody)
		//ToDO: refactor to loops

	}
}
