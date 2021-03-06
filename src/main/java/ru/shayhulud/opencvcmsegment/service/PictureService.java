package ru.shayhulud.opencvcmsegment.service;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritablePixelFormat;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import ru.shayhulud.opencvcmsegment.common.behavior.CONSOLE;
import ru.shayhulud.opencvcmsegment.common.behavior.GUI;
import ru.shayhulud.opencvcmsegment.common.util.CollectionUtil;
import ru.shayhulud.opencvcmsegment.common.util.MathUtil;
import ru.shayhulud.opencvcmsegment.common.util.OutFileNameGenerator;
import ru.shayhulud.opencvcmsegment.common.util.PixelUtil;
import ru.shayhulud.opencvcmsegment.model.BrightLevel;
import ru.shayhulud.opencvcmsegment.model.ImageInfo;
import ru.shayhulud.opencvcmsegment.model.MarkerMap;
import ru.shayhulud.opencvcmsegment.model.OtsuStep;
import ru.shayhulud.opencvcmsegment.model.Result;
import ru.shayhulud.opencvcmsegment.model.dic.AlgorithmOptions;
import ru.shayhulud.opencvcmsegment.model.dic.SegMethod;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Picture processing service.
 */
@Slf4j
public class PictureService {

//  debug mat
//	byte[] b = new byte[showConverted.cols() * showConverted.rows() * showConverted.channels()];
//		showConverted.get(0,0,b);
//		log.info("", b);

	private final Random rnd = new Random();

	//TODO: сделать метод image2Mat
	public Image mat2Image(Mat frame) {
		try {
			return SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
		} catch (Exception e) {
			log.error("Cannot convert the Mat obejct: ", e);
			return null;
		}
	}

	public Image result2Image(Result result) {
		//TODO: добавить зависимость от типа входной матрицы
		if (!result.isNeedToMultiply()) {
			return mat2Image(result.getMat());
		}
		Mat show = result.getMat().clone();
		//Core.multiply(show, new Scalar(result.getMultiplier()), show);
		Mat showConverted = new Mat();
		show.convertTo(show, show.type(), 255, 0);
		show.convertTo(showConverted, CvType.CV_8U);
		return mat2Image(showConverted);
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

	public Mat image2Mat(Image image) {
		int width = (int) image.getWidth();
		int height = (int) image.getHeight();
		byte[] buffer = new byte[width * height * 4];

		PixelReader reader = image.getPixelReader();
		WritablePixelFormat<ByteBuffer> format = WritablePixelFormat.getByteBgraInstance();
		reader.getPixels(0, 0, width, height, format, buffer, 0, width * 4);

		Mat mat = new Mat(height, width, CvType.CV_8UC4);
		mat.put(0, 0, buffer);
		return mat;
	}

	@CONSOLE
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

	@GUI
	public ImageInfo readPicture(File image) throws IOException {
		ImageInfo ii = new ImageInfo();

		String pictureName = image.getName();
		ii.setImageFullFileName(pictureName);

		//Convert file to openCV Mat
		String imageCanonPath = image.getCanonicalPath();
		Mat src = Imgcodecs.imread(imageCanonPath);
		if (src.dataAddr() == 0) {
			throw new IOException();
		}
		ii.setMat(src);
		ii.setResults(new LinkedList<>());
		ii.setImageDir(image.getParent());
		ii.setImageFileName(pictureName.split("\\.")[0]);
		ii.setOutputDirName(ii.getImageFileName() + "_output");
		log.info("read {} file size of: {}; size: {}x{}",
			image.getName(), image.length(), src.cols(), src.rows()
		);
		return ii;
	}

	private String createOutputFolder(String outputDirPath) throws IOException {
		File outputFolder = new File(outputDirPath);
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

	@CONSOLE
	private void saveImage(Mat out, ImageInfo ii, String method, int step, String stepName) throws IOException {
		String fileName = OutFileNameGenerator.generatePng(method + ii.getImageFileName(), step, stepName);
		File result = new File(ii.getOutputDirName() + File.separator + fileName);
		String outputPath = result.getCanonicalPath();
		Imgcodecs.imwrite(outputPath, out);
		log.info("wrote image {}", outputPath);
	}

	@CONSOLE
	private void saveImage(Mat out, ImageInfo ii, String method, int step, String stepName, double multiplier)
		throws IOException {

		Mat multipliedOut = out.clone();
		Core.multiply(multipliedOut, new Scalar(multiplier), multipliedOut);
		saveImage(multipliedOut, ii, method, step, stepName);
	}

	@GUI
	public void saveResultsToFS(ImageInfo ii) {
		String outputDir = "";
		try {
			outputDir = createOutputFolder(ii.getOutputDirPath());
		} catch (IOException e) {
			log.error("Error during create outputDir: ", e);
			return;
		}
		for (Result result : ii.getResults()) {
			try {
				Mat toSave = result.getMat().clone();
				if (result.isNeedToMultiply()) {
					Core.multiply(toSave, new Scalar(result.getMultiplier()), toSave);
				}
				String fileName = OutFileNameGenerator.generatePng(
					ii.getMethod() + "_" + ii.getImageFileName(),
					result.getStep(),
					result.getStepName()
				);
				File resultFile = new File(outputDir + File.separator + fileName);
				String outputPath = resultFile.getCanonicalPath();
				Imgcodecs.imwrite(outputPath, toSave);
				log.info("wrote image {}", outputPath);
			} catch (IOException e) {
				log.error("Error saving result {}", result.getStepName(), e);
				continue;
			}
		}

		if (ii.getTestStats() != null && !ii.getTestStats().isEmpty()) {
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(outputDir + File.separator + "stats.txt"));
				writer.write(ii.getTestStats());
				writer.close();
			} catch (IOException e) {
				log.error("Error saving stats of {}", ii.getImageFileName(), e);
				return;
			}
		}
	}

	private byte[] generateBGRColor() {
		byte b = (byte) (this.rnd.nextInt(156) + 100);
		byte g = (byte) (this.rnd.nextInt(156) + 100);
		byte r = (byte) (this.rnd.nextInt(156) + 100);
		return new byte[]{b, g, r};
	}

	public Result makeResult(Mat out, int step, String stepName) {
		Result result = new Result();
		result.setMat(out);
		result.setStep(step);
		result.setStepName(stepName);
		//log.info("making result with mat of type {}", out.type());
		return result;
	}

	public void saveResult(Mat out, ImageInfo ii, int step, String stepName) {
		ii.getResults().add(makeResult(out, step, stepName));
	}

	public void saveResult(Mat out, ImageInfo ii, int step, String stepName, double multiplier) {
		Result result = makeResult(out, step, stepName);
		result.setNeedToMultiply(true);
		result.setMultiplier(multiplier);
		ii.getResults().add(result);
	}

	//handMarkers = CV_8U
	@GUI
	public ImageInfo handMarkerWatershed(ImageInfo ii, Mat handMarkers) {
		int step = 0;
		ii.setMethod(SegMethod.HAND_METHOD);

		Mat src = ii.getMat().clone();
		saveResult(handMarkers, ii, ++step, "hand_markers");

		List<MatOfPoint> contours = new ArrayList<>();
		MatOfInt4 hierarchy = new MatOfInt4();
		Imgproc.findContours(handMarkers, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_NONE);
		Mat markers = Mat.zeros(handMarkers.size(), CvType.CV_32SC1);
		for (int i = 0; i < contours.size(); i++) {
			Imgproc.drawContours(markers, contours, i, Scalar.all(i + 1), -1, 8, hierarchy, Integer.MAX_VALUE, new Point());
		}
		ii.setDepth(contours.size());
		saveResult(markers.clone(), ii, ++step, "markers", 10000);

		Mat dst = this.watershed(src, markers, ii.getDepth(), true);
		//saveImage(dst, ii, COLOR_METHOD, ++step, "result");
		saveResult(dst.clone(), ii, ++step, "result");

		return ii;
	}

	@CONSOLE
	public ImageInfo colorAutoMarkerWatershed(String picturePath, String outMainFolder, String pictureName) {
		try {
			ImageInfo ii = readPicture(picturePath, outMainFolder, pictureName);
			return colorAutoMarkerWatershed(ii, new HashSet<>());
		} catch (IOException e) {
			log.error("There is an error with file stream processing", e);
			return null;
		}
	}

	@GUI
	public ImageInfo colorAutoMarkerWatershed(ImageInfo ii, Set<AlgorithmOptions> options) {

		boolean colored = options.contains(AlgorithmOptions.COLORED);
		int step = 0;
		ii.setMethod(SegMethod.COLOR_METHOD);

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
		if (!options.contains(AlgorithmOptions.NO_SAVE_STEPS)) {
			saveResult(src.clone(), ii, ++step, "black_bg");
		}

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
		if (!options.contains(AlgorithmOptions.NO_SAVE_STEPS)) {
			saveResult(imgResult.clone(), ii, ++step, "laplassian_sharp");
		}

		Mat bw = this.bwMat(src);
		if (!options.contains(AlgorithmOptions.NO_SAVE_STEPS)) {
			saveResult(bw.clone(), ii, ++step, "bw");
		}

		Mat distance = this.distanceTransform(bw);
		if (!options.contains(AlgorithmOptions.NO_SAVE_STEPS)) {
			saveResult(distance.clone(), ii, ++step, "distance_transform", 1000);
		}

		Imgproc.threshold(distance, distance, .4, 1., Imgproc.THRESH_BINARY);
		Mat kernel1 = Mat.ones(3, 3, CvType.CV_8UC1);
		Imgproc.dilate(distance, distance, kernel1);
		if (!options.contains(AlgorithmOptions.NO_SAVE_STEPS)) {
			saveResult(distance.clone(), ii, ++step, "distance_peaks", 1000);
		}

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
		ii.setDepth(contours.size());
		Imgproc.circle(markers, new Point(5, 5), 3, new Scalar(255, 255, 255), -1);
		//Output markers * 10000
		if (!options.contains(AlgorithmOptions.NO_SAVE_STEPS)) {
			saveResult(markers.clone(), ii, ++step, "markers", 10000);
		}

		Mat dst = this.watershed(src, markers, ii.getDepth(), colored);
		//saveImage(dst, ii, COLOR_METHOD, ++step, "result");
		saveResult(dst.clone(), ii, ++step, "result");

		Mat bwResult = new Mat();
		Imgproc.cvtColor(dst, bwResult, Imgproc.COLOR_BGR2GRAY);
		bwResult.convertTo(bwResult, CvType.CV_8U);
		saveResult(bwResult.clone(), ii, ++step, "bw_result");

		return ii;
	}

	@CONSOLE
	public ImageInfo shapeAutoMarkerWatershed(String picturePath, String outMainFolder, String pictureName) {
		try {
			ImageInfo ii = readPicture(picturePath, outMainFolder, pictureName);
			return shapeAutoMarkerWatershed(ii, new HashSet<>());
		} catch (IOException e) {
			log.error("There is an error with file stream processing", e);
			return null;
		}
	}

	@GUI
	public ImageInfo shapeAutoMarkerWatershed(ImageInfo ii, Set<AlgorithmOptions> options) {

		boolean colored = options.contains(AlgorithmOptions.COLORED);
		int step = 0;
		ii.setMethod(SegMethod.SHAPE_METHOD);

		Mat src = ii.getMat().clone();

		Mat srcGray = new Mat();
		Imgproc.cvtColor(src, srcGray, Imgproc.COLOR_BGR2GRAY);
		srcGray.convertTo(srcGray, CvType.CV_8U);
		Integer medianmaskBlurSize = this.calculateSizeOfSquareBlurMask(srcGray);
		Imgproc.medianBlur(srcGray, srcGray, medianmaskBlurSize);
		if (!options.contains(AlgorithmOptions.NO_SAVE_STEPS)) {
			saveResult(srcGray.clone(), ii, ++step, "blured_by_" + medianmaskBlurSize + "x" + medianmaskBlurSize);
		}

		int lowTreshold = 5;
		int ratio = 10;
		Mat brdGray = srcGray.clone();
		Imgproc.Canny(brdGray, brdGray, lowTreshold, lowTreshold * ratio);
		Mat brdDst = Mat.zeros(src.size(), src.type());
		src.copyTo(brdDst, brdGray);
		if (!options.contains(AlgorithmOptions.NO_SAVE_STEPS)) {
			saveResult(brdDst.clone(), ii, ++step, "borders");
		}
		if (!options.contains(AlgorithmOptions.NO_SAVE_STEPS)) {
			saveResult(brdGray.clone(), ii, ++step, "gray_borders");
		}
		Mat markerMask = brdGray.clone();

		Mat dStep = new Mat(markerMask.size(), markerMask.type());
		Imgproc.dilate(markerMask, dStep, Mat.ones(3, 3, markerMask.type()));
		Imgproc.dilate(dStep, markerMask, Mat.ones(5, 5, markerMask.type()));
		Core.subtract(markerMask, dStep, markerMask);

		if (!options.contains(AlgorithmOptions.NO_SAVE_STEPS)) {
			saveResult(markerMask.clone(), ii, ++step, "dde_step");
		}

		Imgproc.medianBlur(markerMask, markerMask, 3);
		if (!options.contains(AlgorithmOptions.NO_SAVE_STEPS)) {
			saveResult(markerMask.clone(), ii, ++step, "dde_step_blurred_3x3");
		}

		Mat markers = Mat.zeros(markerMask.size(), CvType.CV_32S);
		Imgproc.connectedComponents(markerMask, markers, 8, CvType.CV_32S);
		if (!options.contains(AlgorithmOptions.NO_SAVE_STEPS)) {
			saveResult(markers.clone(), ii, ++step, "markers", 10000);
		}

		List<MatOfPoint> contours = new ArrayList<>();
		MatOfInt4 hierarchy = new MatOfInt4();

		Imgproc.findContours(markerMask, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_NONE);
		if (contours.isEmpty()) {
			log.info("contours is empty");
			return null;
		}
		ii.setDepth(contours.size());

		Mat dst = this.watershed(src, markers, ii.getDepth(), colored);
		//saveImage(dst, ii, SHAPE_METHOD, ++step, "result");
		saveResult(dst.clone(), ii, ++step, "result");

		Mat bwResult = new Mat();
		Imgproc.cvtColor(dst, bwResult, Imgproc.COLOR_BGR2GRAY);
		bwResult.convertTo(bwResult, CvType.CV_8U);
		saveResult(bwResult.clone(), ii, ++step, "bw_result");

		return ii;
	}

	public ImageInfo notConnectedMarkers(ImageInfo ii, Integer depth, Integer filterMaskSize, Set<AlgorithmOptions> options) {

		OffsetDateTime startAlgTime = OffsetDateTime.now();

		boolean colored = options.contains(AlgorithmOptions.COLORED);
		int step = 0;
		ii.setMethod(SegMethod.NOT_CONNECTED_MARKERS);
		Mat src = ii.getMat().clone();

		Mat srcGray = new Mat();
		Imgproc.cvtColor(src, srcGray, Imgproc.COLOR_BGR2GRAY);
		srcGray.convertTo(srcGray, CvType.CV_8U);
		if (options.contains(AlgorithmOptions.MEDIAN_BLUR)) {
			//K<=16
			Imgproc.medianBlur(srcGray, srcGray, filterMaskSize);
			if (!options.contains(AlgorithmOptions.NO_SAVE_STEPS)) {
				saveResult(srcGray.clone(), ii, ++step,
					"blured_by_" + filterMaskSize + "x" + filterMaskSize + "_" + AlgorithmOptions.MEDIAN_BLUR.name());
			}
		} else if (options.contains(AlgorithmOptions.BILATERIAL)) {
			Mat dst = new Mat();
			Imgproc.bilateralFilter(srcGray, dst, filterMaskSize, filterMaskSize * 2, filterMaskSize * 2);
			if (!options.contains(AlgorithmOptions.NO_SAVE_STEPS)) {
				saveResult(dst.clone(), ii, ++step,
					"blured_by_" + filterMaskSize + "x" + filterMaskSize + "_" + AlgorithmOptions.BILATERIAL.name());
			}
			dst.convertTo(srcGray, CvType.CV_8U);
		}

		log.info("srcGray type of: {}; channels: {}", srcGray.type(), srcGray.channels());

		//COLOR HISTOS
		++step;
		float[] range = {0f, 256f};

		int hist_w = 1024;
		int hist_h = 1024;
		int histSize = 256;
		Mat histImage = new Mat(hist_w, hist_h, CvType.CV_8UC3, new Scalar(0, 0, 0));
		int bin_w = Double.valueOf(hist_w / histSize).intValue();

		List<Mat> bgrPanes = new LinkedList<>();
		List<Mat> hists = new LinkedList<>();
		Core.split(src, bgrPanes);
		for (int i = 0; i < bgrPanes.size(); i++) {
			Mat pane = bgrPanes.get(i);

			Mat hist = new Mat(hist_w, hist_h, CvType.CV_8UC3, new Scalar(0, 0, 0));
			Imgproc.calcHist(Arrays.asList(pane), new MatOfInt(0), new Mat(), hist, new MatOfInt(histSize), new MatOfFloat(range));
			Core.normalize(hist, hist, 0, histImage.rows(), Core.NORM_MINMAX, -1, new Mat());
			hists.add(hist);
		}

		for (int i = 1; i < histSize; i++) {
			Imgproc.line(
				histImage,
				new Point(bin_w * (i - 1), hist_h - (hists.get(0).get(i - 1, 0)[0])),
				new Point(bin_w * (i), hist_h - (hists.get(0).get(i, 0)[0])),
				new Scalar(255, 0, 0), 2, 8, 0
			);
			Imgproc.line(
				histImage,
				new Point(bin_w * (i - 1), hist_h - (hists.get(1).get(i - 1, 0)[0])),
				new Point(bin_w * (i), hist_h - (hists.get(1).get(i, 0)[0])),
				new Scalar(0, 255, 0), 2, 8, 0
			);
			Imgproc.line(
				histImage,
				new Point(bin_w * (i - 1), hist_h - (hists.get(2).get(i - 1, 0)[0])),
				new Point(bin_w * (i), hist_h - (hists.get(2).get(i, 0)[0])),
				new Scalar(0, 0, 255), 2, 8, 0
			);
		}
		Mat histOut = histImage.clone();
		histOut.convertTo(histOut, CvType.CV_8SC3);
		if (!options.contains(AlgorithmOptions.NO_SAVE_STEPS)) {
			saveResult(histOut.clone(), ii, step, "bgr_histo");
		}

		//BRIGHT HISTO
		++step;
		Mat brightHist = new Mat(hist_w, hist_h, CvType.CV_8UC1, new Scalar(0, 0, 0));
		Imgproc.calcHist(Arrays.asList(srcGray), new MatOfInt(0), new Mat(), brightHist, new MatOfInt(histSize), new MatOfFloat(range));

		//OUTPUT BRIGHTHISTO
		if (!options.contains(AlgorithmOptions.NO_SAVE_STEPS)) {
			saveOutputBrightHisto(ii, step, brightHist, hist_h, hist_w);
		}

		log.info("bright hist size {}", brightHist.size());
		Mat reducedBrightHist = Mat.zeros(brightHist.rows() / 2, brightHist.cols(), brightHist.type());
		for (int i = 0; i < histSize; i += 2) {
			int originalHistValue_1 = (int) brightHist.get(i, 0)[0];
			int originalHistValue_2 = (int) brightHist.get(i + 1, 0)[0];
			reducedBrightHist.put(i / 2, 0, originalHistValue_1 + originalHistValue_2);
		}

		int reducedHistSize = reducedBrightHist.rows();

		log.info("reduced btight hist size {}", reducedBrightHist.size());

		//OUTPUT REDUCED BRIGHTHISTO
		if (!options.contains(AlgorithmOptions.NO_SAVE_STEPS)) {
			saveOutputBrightHisto(ii, step, reducedBrightHist, hist_h, hist_w);
		}

		//Collecting ranges

		OffsetDateTime startMeanAllocateTime = OffsetDateTime.now();

		int blockSizeLimit = Double.valueOf(256 / depth).intValue();
		log.info("max block size = {}", blockSizeLimit);
		List<BrightLevel> fThresholds = new LinkedList<>();
		List<Integer> meanFlexThresholdBlock = new LinkedList<>();
		int startHistValue = (int) brightHist.get(0, 0)[0];
		meanFlexThresholdBlock.add(startHistValue);
		BrightLevel temp = new BrightLevel(0, 0, startHistValue);
		for (int i = 1; i < histSize; i++) {
			int prev = (int) brightHist.get(i - 1, 0)[0];
			int curr = (int) brightHist.get(i, 0)[0];

			if (curr == 0) {
				if (prev != 0) {
					temp.setEnd(i - 1);
					fThresholds.add(temp.clone());
				}

				temp = new BrightLevel(i, i, curr);
				meanFlexThresholdBlock = new LinkedList<>();
				continue;
			}

			if (prev == 0) {
				temp = new BrightLevel(i, i, curr);
			}

			List<Integer> meanListWithCurrent = new LinkedList<>(meanFlexThresholdBlock);
			meanListWithCurrent.add(curr);

			//TODO: вынести в инпут параметры
			double coeff = 0.5;
			int oldMean = meanFlexThresholdBlock.isEmpty() ? curr : MathUtil.meanI(meanFlexThresholdBlock);
			int newMean = MathUtil.meanI(meanListWithCurrent);
			double min_t = oldMean - oldMean * coeff;
			double max_t = oldMean + oldMean * coeff;

			if (min_t <= newMean && newMean <= max_t) {
				if (meanFlexThresholdBlock.size() >= blockSizeLimit) {
					temp.setEnd(i - 1);
					fThresholds.add(temp.clone());

					temp = new BrightLevel(i, i, curr);
					meanFlexThresholdBlock = new LinkedList<>();
					meanFlexThresholdBlock.add(curr);
					continue;
				} else {
					meanFlexThresholdBlock.add(curr);
					temp.setCount(temp.getCount() + curr);
					continue;
				}
			} else {
				temp.setEnd(i - 1);
				fThresholds.add(temp.clone());

				temp = new BrightLevel(i, i, curr);
				meanFlexThresholdBlock = new LinkedList<>();
				meanFlexThresholdBlock.add(curr);
			}
		}
		log.info("flex thresholds:{} size {}", fThresholds, fThresholds.size());

		OffsetDateTime stopMeanAllocateTime = OffsetDateTime.now();
		Duration meanAllocatePeriod = Duration.between(startMeanAllocateTime, stopMeanAllocateTime);
		log.info(
			"{} alg completed in {} min {} sec {} mills",
			"Mean Allocate Blocks",
			meanAllocatePeriod.get(ChronoUnit.SECONDS) / 60,
			meanAllocatePeriod.get(ChronoUnit.SECONDS) % 60,
			meanAllocatePeriod.get(ChronoUnit.NANOS) / 1000000
		);

		if (options.contains(AlgorithmOptions.MULTI_OTSU)) {

			OffsetDateTime startOtsuAllocateTime = OffsetDateTime.now();

			int numberOfThresholds = fThresholds.size();

			int pixNum = srcGray.rows() * srcGray.cols();
			double mt = 0;
			double[] wk = new double[numberOfThresholds + 1];
			Arrays.fill(wk, 0);
			double[] mk = new double[numberOfThresholds + 1];
			Arrays.fill(mk, 0);
			double[] m = new double[numberOfThresholds + 1];
			Arrays.fill(m, 0);

			for (int i = 0; i < reducedHistSize; i++) {
				int intensity = (int) reducedBrightHist.get(i, 0)[0];
				mt += i * (intensity / (double) pixNum);
			}

			OtsuStep otsuThresholds = otsuPart(numberOfThresholds, wk, mk, m, mt, pixNum, reducedHistSize, reducedBrightHist, 0, 0);
			log.info("otsu thresholds {}", otsuThresholds);

			List<Integer> thresholds = otsuThresholds.getDeepers();
			thresholds.add(otsuThresholds.getIdxMaxVar());

			thresholds.sort(Integer::compareTo);

			//ScaleUpBack
			thresholds = thresholds.stream().
				map(th -> {
					int nVal = th * (histSize / reducedHistSize);
					return nVal >= histSize ? histSize - 1 : nVal;
				}).collect(Collectors.toList());

			List<BrightLevel> overrrideFthresholds = new LinkedList<>();

			int begin = 0;
			for (Integer threshold : thresholds) {
				BrightLevel override = new BrightLevel(begin, threshold - 1, 0);
				begin = threshold;
				overrrideFthresholds.add(override);
			}
			BrightLevel last = CollectionUtil.getLastOf(overrrideFthresholds);
			last.setEnd(histSize - 1);

			for (int i = 0; i < histSize; i++) {
				int curr = (int) brightHist.get(i, 0)[0];
				for (BrightLevel overrride : overrrideFthresholds) {
					if (overrride.getStart() <= i && i <= overrride.getEnd()) {
						overrride.setCount(overrride.getCount() + curr);
					}
				}
			}

			log.info("overrideFthresholds {}", overrrideFthresholds);

			OffsetDateTime stopOtsuAllocateTime = OffsetDateTime.now();
			Duration otsuAllocatePeriod = Duration.between(startOtsuAllocateTime, stopOtsuAllocateTime);
			log.info(
				"{} alg completed in {} min {} sec {} mills",
				"Otsu Allocate Blocks",
				otsuAllocatePeriod.get(ChronoUnit.SECONDS) / 60,
				otsuAllocatePeriod.get(ChronoUnit.SECONDS) % 60,
				otsuAllocatePeriod.get(ChronoUnit.NANOS) / 1000000
			);

			fThresholds = overrrideFthresholds;
		}

		int maxBlockValue = fThresholds.stream()
			.map(BrightLevel::getCount)
			.max(Integer::compareTo).get();

		Mat fBrightHistImage = new Mat(hist_w, hist_h, CvType.CV_8UC3, new Scalar(0, 0, 0));
		int fbbin_w = Double.valueOf(hist_w / (histSize)).intValue();
		for (BrightLevel brightLevel : fThresholds) {
			double levelHeightNorm = MathUtil.normalize((double) brightLevel.getCount(), (double) maxBlockValue, (double) hist_h);
			double levelStartNorm = brightLevel.getStart() * fbbin_w;
			double levelEndNorm = (brightLevel.getEnd() + 1) * fbbin_w;

			Imgproc.rectangle(fBrightHistImage,
				new Point(levelStartNorm, hist_h - levelHeightNorm),
				new Point(levelEndNorm, hist_h),
				new Scalar(170, 170, 170),
				-1
			);

			double fontScale = 1;
			int fontFace = Core.FONT_HERSHEY_SCRIPT_SIMPLEX;
			int thickness = 3;

			Size textSize = Imgproc.getTextSize(
				brightLevel.getRangeString(), fontFace, fontScale, thickness, new int[]{0}
			);

			double textLevelRatio = textSize.width / (levelEndNorm - levelStartNorm);
			fontScale = textLevelRatio > 1 ? 1 / textLevelRatio : 1;

			//Рисуем там, где умещается.
			if (fontScale < 0.5) {
				continue;
			}

			if (0.5 <= fontScale && fontScale < 1) {
				thickness = 1;
				textSize = Imgproc.getTextSize(
					brightLevel.getRangeString(), fontFace, fontScale, thickness, new int[]{0}
				);
			}

			Imgproc.putText(fBrightHistImage, brightLevel.getRangeString(),
				new Point(
					levelStartNorm + (levelEndNorm - levelStartNorm) / 2 - textSize.width / 2,
					hist_h - levelHeightNorm / 2
				),
				fontFace, fontScale, Scalar.all(255), thickness
			);
		}
		if (!options.contains(AlgorithmOptions.NO_SAVE_STEPS)) {
			saveResult(fBrightHistImage.clone(), ii, step, "flex_bright_thresholds");
		}

		//SHOW BRIGHTS LVEVEL

		//MAKE LAYERS
		++step;
		List<MarkerMap> meanMarkerMaps = new LinkedList<>();
		for (int i = 0; i < fThresholds.size(); i++) {
			MarkerMap mm = new MarkerMap(i + 1, fThresholds.get(i), srcGray.size(), srcGray.type());
			meanMarkerMaps.add(mm);
		}

		//ALLOCATE TO LAYERS
		for (int i = 0; i < srcGray.rows(); i++) {
			for (int j = 0; j < srcGray.cols(); j++) {
				short brightness = (short) srcGray.get(i, j)[0];
				for (MarkerMap markerMap : meanMarkerMaps) {
					BrightLevel currBrightLevel = markerMap.getBrightLevel();
					if (currBrightLevel.getStart() <= brightness && brightness <= currBrightLevel.getEnd()) {
						markerMap.getAllLevel().put(i, j, new byte[]{(byte) 255});
						markerMap.incrementAllLevelCount();
					}

					if (options.contains(AlgorithmOptions.GISTO_DIAP)) {
						//TODO: Сделать вводимым/дефолтным.
						int diapRange = 3;
						BrightLevel meanDiap = currBrightLevel.getMeanDiap(diapRange);
						if (meanDiap.getStart() <= brightness && brightness <= meanDiap.getEnd()) {
							markerMap.getMeanLevel().put(i, j, new byte[]{(byte) 255});
							markerMap.incrementMeanLevelCount();

							double[] indexes = new double[]{markerMap.getIdx()};
							markerMap.getMarker().put(i, j, indexes);
							markerMap.incrementMarkerLevelCount();
							break;
						}
					} else {
						int mean = currBrightLevel.getMeanLevel();
						if (brightness == mean) {
							markerMap.getMeanLevel().put(i, j, new byte[]{(byte) 255});
							markerMap.incrementMeanLevelCount();

							double[] indexes = new double[]{markerMap.getIdx()};
							markerMap.getMarker().put(i, j, indexes);
							markerMap.incrementMarkerLevelCount();
							break;
						}
					}
				}
			}
		}

		if (!options.contains(AlgorithmOptions.NO_SAVE_STEPS)) {
			for (MarkerMap markerMap : meanMarkerMaps) {
				int _idx = markerMap.getIdx();
				saveResult(markerMap.getAllLevel().clone(), ii, step, "mean_thresh_level_" + _idx + "_of_depth");
				saveResult(markerMap.getMeanLevel().clone(), ii, step, "mean_level_" + _idx + "_of_depth", 1);
				saveResult(markerMap.getMarker().clone(), ii, step, "mean_markers_of_level_" + _idx + "_of_depth", 1000);
			}
		}

		for (MarkerMap mm : meanMarkerMaps) {
			log.info("{}", mm.stats());
		}

		Mat wshedMarkSumm = meanMarkerMaps.get(0).getMarker().clone();
		if (meanMarkerMaps.size() > 1) {
			for (int i = 1; i < meanMarkerMaps.size(); i++) {
				MarkerMap markerMap = meanMarkerMaps.get(i);
				Core.add(wshedMarkSumm, markerMap.getMarker(), wshedMarkSumm);
			}
		}
		if (!options.contains(AlgorithmOptions.NO_SAVE_STEPS)) {
			saveResult(wshedMarkSumm.clone(), ii, ++step, "markers_summ", 10000);
		}

		Mat coloredMarkers = colorByIndexes(wshedMarkSumm.clone(), meanMarkerMaps.size(), true);
		if (!options.contains(AlgorithmOptions.NO_SAVE_STEPS)) {
			saveResult(coloredMarkers.clone(), ii, ++step, "colored_markers_summ");
		}

		Mat dst = this.watershed(src, wshedMarkSumm, meanMarkerMaps.size(), colored);

		String resultOptions = options.stream().map(AlgorithmOptions::name).collect(Collectors.joining("_"));
		saveResult(dst.clone(), ii, ++step,
			"result_markers_summ_" + resultOptions);

		Mat bwResult = new Mat();
		Imgproc.cvtColor(dst, bwResult, Imgproc.COLOR_BGR2GRAY);
		bwResult.convertTo(bwResult, CvType.CV_8U);
		saveResult(bwResult.clone(), ii, ++step,
			"bw_result_markers_summ_" + resultOptions);

		OffsetDateTime stopAlgTime = OffsetDateTime.now();
		Duration period = Duration.between(startAlgTime, stopAlgTime);
		log.info(
			"{} alg completed in {} min {} sec {} mills",
			SegMethod.NOT_CONNECTED_MARKERS,
			period.get(ChronoUnit.SECONDS) / 60,
			period.get(ChronoUnit.SECONDS) % 60,
			period.get(ChronoUnit.NANOS) / 1000000
		);

		return ii;
	}

	public Integer calculateSizeOfSquareBlurMask(Mat src) {
		int minOfSrcSize = src.cols() <= src.rows() ? src.cols() : src.rows();
		int minMaskSize = 3;
		double scale;
		if (minOfSrcSize < minMaskSize) {
			return 1;
		} else if (minOfSrcSize <= 100) {
			return 5;
		} else if (minOfSrcSize <= 360) {
			scale = 0.025;
		} else if (minOfSrcSize <= 480) {
			scale = 0.02;
		} else if (minOfSrcSize <= 720) {
			scale = 0.015;
		} else if (minOfSrcSize <= 1080) {
			scale = 0.01;
		} else {
			scale = 0.005;
		}
		int result = Double.valueOf(minOfSrcSize * scale).intValue();
		result = result % 2 == 0 ? result + 1 : result;
		return result;
	}

	public Integer calculatePercentAreaOfImage(Mat src, double percent) {
		int percentH = Double.valueOf((src.cols() * percent) / 100).intValue();
		int percentV = Double.valueOf((src.rows() * percent) / 100).intValue();
		int area = percentH * percentV;
		return area;
	}

	public Mat watershed(Mat src, Mat markers, Integer depth, boolean colored) {
		Imgproc.watershed(src, markers);
		return colorByIndexes(markers, depth, colored);
	}

	private Mat colorByIndexes(Mat markers, Integer depth, boolean colored) {
		List<byte[]> colors = new ArrayList<>();
		for (int i = 0; i < depth; i++) {
			if (colored) {
				colors.add(generateBGRColor());
			} else {
				colors.add(new byte[]{(byte) 255, (byte) 255, (byte) 255});
			}
		}
		byte[] backgroundColor = new byte[]{0, 0, 0};

		Mat dst = new Mat(markers.size(), CvType.CV_8UC3);
		for (int i = 0; i < markers.rows(); i++) {
			for (int j = 0; j < markers.cols(); j++) {
				int index = (int) markers.get(i, j)[0];
				if (index > 0 && index <= depth) {
					dst.put(i, j, colors.get(index - 1));
				} else {
					dst.put(i, j, backgroundColor);
				}
			}
		}
		return dst;
	}

	public Mat bwMat(Mat src) {
		Mat bw = new Mat();
		Imgproc.cvtColor(src, bw, Imgproc.COLOR_BGR2GRAY);
		Imgproc.threshold(bw, bw, 40, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
		return bw;
	}

	private OtsuStep otsuPart(int optimalThresholdsNum, double[] wk, double[] mk, double[] m,
	                          double mt, int pixNum, int histSize, Mat hist, int start, int step) {
		if (step >= optimalThresholdsNum) {
			return null;
		}

		double maxVar = 0;
		int maxVarIdx = start;
		int maxVarDeepIdx = 0;
		List<Integer> maxDeepers = new LinkedList<>();
		for (int ii = start; ii < histSize; ii++) {
			int intensity = (int) hist.get(ii, 0)[0];
			wk[step] += intensity / (double) pixNum;
			mk[step] += ii * intensity / (double) pixNum;
			m[step] = mk[step] / wk[step];

			if (step > 0) {
				double wkSumm = 0;
				double mkSumm = 0;
				for (int k = 0; k < step + 1; k++) {
					wkSumm += wk[k];
					mkSumm += mk[k];
				}
				wk[step + 1] = 1 - wkSumm;
				mk[step + 1] = mt - mkSumm;
			}

			if (step == optimalThresholdsNum - 1) {
				double currVarB = 0;
				for (int k = 0; k < step + 1; k++) {
					currVarB += wk[k] * (m[k] - mt) * (m[k] - mt);
				}
				if (maxVar < currVarB) {
					maxVar = currVarB;
					maxVarIdx = ii;
				}
			} else {
				OtsuStep deeper = otsuPart(optimalThresholdsNum, wk, mk, m, mt, pixNum, histSize, hist, ii + 1, step + 1);
				if (deeper.getMaxVar() > maxVar) {
					maxVarIdx = ii;
					maxVar = deeper.getMaxVar();
					maxVarDeepIdx = deeper.getIdxMaxVar();
					maxDeepers = deeper.getDeepers();
				}
			}
		}
		if (step == optimalThresholdsNum - 1) {
			return new OtsuStep(maxVar, maxVarIdx, new LinkedList<>());
		}
		maxDeepers.add(maxVarDeepIdx);
		return new OtsuStep(maxVar, maxVarIdx, maxDeepers);
	}

	private void saveOutputBrightHisto(ImageInfo ii, int step, Mat brightHist, int hist_h, int hist_w) {
		Mat brightHistOut = brightHist.clone();
		int histSize = brightHistOut.rows();

		Mat brightHistImage = new Mat(hist_w, hist_h, CvType.CV_8UC3, new Scalar(0, 0, 0));

		Core.normalize(brightHistOut, brightHistOut, 0, brightHistImage.rows(), Core.NORM_MINMAX, -1, new Mat());

		int bbin_w = Double.valueOf(hist_w / (histSize)).intValue();
		for (int i = 1; i < histSize; i++) {
			Imgproc.rectangle(brightHistImage,
				new Point(bbin_w * (i - 1), hist_h - (brightHistOut.get(i - 1, 0)[0])),
				new Point(bbin_w * (i), hist_h),
				new Scalar(170, 170, 170),
				-1
			);
		}
		saveResult(brightHistImage.clone(), ii, step, "bright_histo_" + histSize + "_bins");
	}

	public Mat distanceTransform(Mat bw) {
		Mat distance = new Mat();
		Imgproc.distanceTransform(bw, distance, Imgproc.CV_DIST_L2, 5);
		Core.normalize(distance, distance, 0, 1., Core.NORM_MINMAX);
		return distance;
	}
}
