package ru.shayhulud.opencvcmsegment.service;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
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
import ru.shayhulud.opencvcmsegment.model.Result;
import ru.shayhulud.opencvcmsegment.util.DateUtils;
import ru.shayhulud.opencvcmsegment.util.OutFileNameGenerator;
import ru.shayhulud.opencvcmsegment.util.PixelUtil;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
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

	public Image mat2Image(Mat frame) {
		try {
			return SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
		} catch (Exception e) {
			System.err.println("Cannot convert the Mat obejct: " + e);
			return null;
		}
	}

	private BufferedImage matToBufferedImage(Mat m) {
		int type = BufferedImage.TYPE_BYTE_GRAY;
		if (m.channels() > 1) {
			type = BufferedImage.TYPE_3BYTE_BGR;
		}
		int bufferSize = m.channels() * m.cols() * m.rows();
		byte[] b = new byte[bufferSize];
		m.get(0, 0, b); // get all the pixels
		BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(b, 0, targetPixels, 0, b.length);
		return image;
	}

	public ImageInfo readPicture(String picturePath, String outMainFolder, String pictureName) throws IOException {

		String pictureFullPath = picturePath + File.separator + pictureName;
		log.info("pictureFullPath = {}", pictureFullPath);

		//load image content to temp file.
		File image = new File(pictureFullPath);

		//Convert file to openCV Mat
		ImageInfo ii = readPicture(image);

		//TODO: make save button
		//String outDirName = createOutputFolder(picturePath, outMainFolder);
		//ii.setOutputDirName(outDirName);
		log.info("read {} file size of: {}; size: {}x{}",
			image.getName(), image.length(), ii.getMat().cols(), ii.getMat().rows()
		);
		return ii;
	}

	public ImageInfo readPicture(File image) throws IOException {
		ImageInfo ii = new ImageInfo();

		String pictureName = image.getName();
		ii.setImageFileName(pictureName);

		//Convert file to openCV Mat
		Mat src = Imgcodecs.imread(image.getCanonicalPath());
		if (src.dataAddr() == 0) {
			throw new IOException();
		}
		ii.setMat(src);
		ii.setResults(new LinkedList<>());
		log.info("read {} file size of: {}; size: {}x{}",
			image.getName(), image.length(), src.cols(), src.rows()
		);
		return ii;
	}

	@Deprecated
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

	private void saveImage(Mat out, ImageInfo ii, String method, int step, String stepName) throws IOException {
		String fileName = OutFileNameGenerator.generatePng(method + ii.getImageFileName(), step, stepName);
		File result = new File(ii.getOutputDirName() + File.separator + fileName);
		String outputPath = result.getCanonicalPath();
		Imgcodecs.imwrite(outputPath, out);
		log.info("wrote image {}", outputPath);
	}

	private void saveImage(Mat out, ImageInfo ii, String method, int step, String stepName, double multiplier)
		throws IOException {

		Mat multipliedOut = out.clone();
		Core.multiply(multipliedOut, new Scalar(multiplier), multipliedOut);
		saveImage(multipliedOut, ii, method, step, stepName);
	}

	private byte[] generateBGRColor() {
		byte b = (byte) this.rnd.nextInt(256);
		byte g = (byte) this.rnd.nextInt(256);
		byte r = (byte) this.rnd.nextInt(256);
		return new byte[]{b, g, r};
	}

	private Result makeResult(Mat out, int step, String stepName) {
		Result result = new Result();
		result.setMat(out);
		result.setStep(step);
		result.setStepName(stepName);
		return result;
	}

	private void saveResult(Mat out, ImageInfo ii, int step, String stepName) {
		ii.getResults().add(makeResult(out, step, stepName));
	}

	private void saveResult(Mat out, ImageInfo ii, int step, String stepName, double multiplier) {
		Result result = makeResult(out, step, stepName);
		result.setNeedToMultiply(true);
		result.setMultiplier(multiplier);
		ii.getResults().add(result);
	}

	public ImageInfo colorAutoMarkerWatershed(String picturePath, String outMainFolder, String pictureName) {
		try {
			ImageInfo ii = readPicture(picturePath, outMainFolder, pictureName);
			return colorAutoMarkerWatershed(ii);
		} catch (IOException e) {
			log.error("There is an error with file stream processing", e);
			return null;
		}
	}


	public ImageInfo colorAutoMarkerWatershed(ImageInfo ii) {

		int step = 0;
		ii.setMethod(COLOR_METHOD);

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
		//saveImage(src, ii, COLOR_METHOD, ++step, "black_bg");
		saveResult(src.clone(), ii, ++step, "black_bg");

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
		//saveImage(imgResult, ii, COLOR_METHOD, ++step, "laplassian_sharp");
		saveResult(imgResult.clone(), ii, ++step, "laplassian_sharp");

		Mat bw = new Mat();
		Imgproc.cvtColor(src, bw, Imgproc.COLOR_BGR2GRAY);
		Imgproc.threshold(bw, bw, 40, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
		//saveImage(bw, ii, COLOR_METHOD, ++step, "bw");
		saveResult(bw.clone(), ii, ++step, "bw");

		Mat distance = new Mat();
		Imgproc.distanceTransform(bw, distance, Imgproc.CV_DIST_L2, 5);
		Core.normalize(distance, distance, 0, 1., Core.NORM_MINMAX);
		//saveImage(distance, ii, COLOR_METHOD, ++step, "distance_transform", 1000);
		saveResult(distance.clone(), ii, ++step, "distance_transform", 1000);

		Imgproc.threshold(distance, distance, .4, 1., Imgproc.THRESH_BINARY);
		Mat kernel1 = Mat.ones(3, 3, CvType.CV_8UC1);
		Imgproc.dilate(distance, distance, kernel1);
		//saveImage(distance, ii, COLOR_METHOD, ++step, "distance_peaks", 1000);
		saveResult(distance.clone(), ii, ++step, "distance_peaks", 1000);

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
		//saveImage(markers, ii, COLOR_METHOD, ++step, "markers", 10000d);
		saveResult(markers.clone(), ii, ++step, "markers", 10000);

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
				int index = (int) markers.get(i, j)[0];
				if (index > 0 && index <= contours.size()) {
					dst.put(i, j, colors.get(index - 1));
				} else {
					dst.put(i, j, backroundColor);
				}
			}
		}
		//saveImage(dst, ii, COLOR_METHOD, ++step, "result");
		saveResult(dst.clone(), ii, ++step, "result");

		return ii;
	}

	public ImageInfo shapeAutoMarkerWatershed(String picturePath, String outMainFolder, String pictureName) {

		try {
			int step = 0;

			ImageInfo ii = readPicture(picturePath, outMainFolder, pictureName);
			ii.setMethod(SHAPE_METHOD);

			Mat src = ii.getMat().clone();

			Mat markerMask = new Mat();
			Mat srcGray = new Mat();
			Imgproc.cvtColor(src, markerMask, Imgproc.COLOR_BGR2GRAY);
			Imgproc.cvtColor(markerMask, srcGray, Imgproc.COLOR_GRAY2BGR);
			markerMask = Mat.zeros(markerMask.size(), markerMask.type());
			//saveImage(srcGray, ii, SHAPE_METHOD, ++step, "grayed");
			saveResult(srcGray.clone(), ii, ++step, "grayed");

			//TODO: lowTreshold need to be various;
			int lowTreshold = 5;
			int ratio = 10;
			Mat brdGray = src.clone();
			Imgproc.cvtColor(src, brdGray, Imgproc.COLOR_BGR2GRAY);
//			Imgproc.blur(brdGray, brdGray, new Size(5, 5));
			Imgproc.Canny(brdGray, brdGray, lowTreshold, lowTreshold * ratio);
			Mat brdDst = Mat.zeros(src.size(), src.type());
			src.copyTo(brdDst, brdGray);
			//saveImage(brdDst, ii, SHAPE_METHOD, ++step, "borders");
			saveResult(brdDst.clone(), ii, ++step, "borders");
			//saveImage(brdGray, ii, SHAPE_METHOD, ++step, "gray_borders");
			saveResult(brdGray.clone(), ii, ++step, "gray_borders");
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
			//horizontal
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
			//RVH
			Mat rvhKernel = this.create3x3Kernel(markerMask.type(), new int[]{
				0, 0, 1,
				0, 1, 0,
				1, 0, 0});
			Mat mmRVH = markerMask.clone();
			Imgproc.erode(mmRVH, mmRVH, rvhKernel);
			Imgproc.dilate(mmRVH, mmRVH, rvhKernel);

			//summ
			Core.add(mmH, mmV, mmH);
			Core.add(mmLVH, mmRVH, mmLVH);
			Core.add(mmH, mmLVH, mmH);
			//saveImage(mmH, ii, SHAPE_METHOD, ++step, "ALL_smaler");
			saveResult(mmH.clone(), ii, ++step, "ALL_smaler");

			Imgproc.dilate(mmH, mmH, Mat.ones(5, 5, markerMask.type()));
			Imgproc.erode(mmH, mmH, Mat.ones(5, 5, markerMask.type()));
			//saveImage(mmH, ii, SHAPE_METHOD, ++step, "open_smaler");
			saveResult(mmH.clone(), ii, ++step, "open_smaler");

			mmH.copyTo(markerMask);

//			Mat median = markerMask.clone();
//			Imgproc.medianBlur(mmH, mmH, 3);
//			saveImage(mmH, ii, SHAPE_METHOD, "_median_remove_smaler.png");
//			Core.add(median, mmH, median);
//			saveImage(median, ii, SHAPE_METHOD, "_V+H+M_remove_smaler.png");

			List<MatOfPoint> contours = new ArrayList<>();
			MatOfInt4 hierarchy = new MatOfInt4();

			Imgproc.findContours(markerMask, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_NONE);
			if (contours.isEmpty()) {
				log.info("contours is empty");
				return null;
			}

			Mat markers = Mat.zeros(markerMask.size(), CvType.CV_32S);
			for (int i = 0; i < contours.size(); i++) {
				Imgproc.drawContours(markers, contours, i, Scalar.all(i + 1), 3, 8, hierarchy, Integer.MAX_VALUE, new Point());
			}
			//TODO:Добавить еще одну итерацию с выделением контуров.
			//markerMask.convertTo(markers, CvType.CV_32S);
			Imgproc.circle(markers, new Point(5, 5), 3, new Scalar(255, 255, 255), -1);

			//saveImage(markers, ii, SHAPE_METHOD, ++step, "markers", 10000);
			saveResult(markers.clone(), ii, ++step, "markers", 10000);

			List<byte[]> colors = new ArrayList<>();
			for (int i = 0; i < contours.size(); i++) {
				colors.add(generateBGRColor());
			}
			byte[] backroundColor = generateBGRColor();

			Imgproc.watershed(src, markers);

			Mat dst = new Mat(markers.size(), CvType.CV_8UC3);
			for (int i = 0; i < markers.rows(); i++) {
				for (int j = 0; j < markers.cols(); j++) {
					int index = (int) markers.get(i, j)[0];
					if (index > 0 && index <= contours.size()) {
						dst.put(i, j, colors.get(index - 1));
					} else {
						dst.put(i, j, backroundColor);
					}
				}
			}
			//saveImage(dst, ii, SHAPE_METHOD, ++step, "result");
			saveResult(dst.clone(), ii, ++step, "result");
			return ii;

		} catch (IOException e) {
			log.error("There is an error with file stream processing", e);
		}
		return null;
	}

	private Mat create3x3Kernel(int type, int[] kernelBody) {
		if (kernelBody.length != 9) {
			throw new IncorrectMatBodyLengthException("wrong kernel length");
		}

		double[] kernelBodyD = new double[kernelBody.length];
		for (int i = 0; i < kernelBody.length; i++) {
			kernelBodyD[i] = (double) kernelBody[i];
		}

		Mat kernel = new Mat(3, 3, type, new Scalar(kernelBodyD));
		return kernel;
		//ToDO: refactor to loops for custom kernel size

	}
}
