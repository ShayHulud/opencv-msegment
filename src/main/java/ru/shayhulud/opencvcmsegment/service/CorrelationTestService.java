package ru.shayhulud.opencvcmsegment.service;

import lombok.extern.slf4j.Slf4j;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import ru.shayhulud.opencvcmsegment.common.util.CollectionUtil;
import ru.shayhulud.opencvcmsegment.model.ImageInfo;
import ru.shayhulud.opencvcmsegment.model.dic.AlgorithmOptions;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Correlation testing service.
 */
@Slf4j
public class CorrelationTestService {

	private final PictureService pictureService = new PictureService();

	private final List<Set<AlgorithmOptions>> optionsList = new LinkedList<Set<AlgorithmOptions>>() {{
		add(new HashSet<>(Arrays.asList(AlgorithmOptions.NO_SAVE_STEPS, AlgorithmOptions.MEDIAN_BLUR)));
		add(new HashSet<>(Arrays.asList(AlgorithmOptions.NO_SAVE_STEPS, AlgorithmOptions.MEDIAN_BLUR, AlgorithmOptions.GISTO_DIAP)));
		add(new HashSet<>(Arrays.asList(AlgorithmOptions.NO_SAVE_STEPS, AlgorithmOptions.BILATERIAL)));
		add(new HashSet<>(Arrays.asList(AlgorithmOptions.NO_SAVE_STEPS, AlgorithmOptions.BILATERIAL, AlgorithmOptions.GISTO_DIAP)));
		add(new HashSet<>(Arrays.asList(AlgorithmOptions.NO_SAVE_STEPS)));
		add(new HashSet<>(Arrays.asList(AlgorithmOptions.NO_SAVE_STEPS, AlgorithmOptions.GISTO_DIAP)));
	}};

	private final List<Integer> depths = Arrays.asList(2, 3, 4, 5, 6);
	private final List<Integer> maskSizes = Arrays.asList(3, 5, 7);

	public double getSimilarity(Mat algthResult, Mat humanResult) {

		Mat result = Mat.zeros(algthResult.size(), CvType.CV_32S);
		Imgproc.matchTemplate(algthResult, humanResult, result, Imgproc.TM_CCORR_NORMED);
		double index = (double) result.get(0, 0)[0];
		return index;
	}

	public void massTest(Map<ImageInfo, ImageInfo> tests) {
		tests.forEach(
			(_k, _v) -> this.pictureService.saveResultsToFS(this.test(_k, _v))
		);
	}

	public ImageInfo test(ImageInfo iiSrc, ImageInfo iiHumanRes) {

		ImageInfo result = new ImageInfo();
		result.setResults(new LinkedList<>());
		result.setImageDir(iiSrc.getImageDir());
		result.setImageFileName(iiSrc.getImageFileName());
		result.setImageFullFileName(iiSrc.getImageFullFileName());
		result.setOutputDirName(iiSrc.getOutputDirName());

		int step = 0;

		Mat src = iiSrc.getMat();
		this.pictureService.saveResult(src.clone(), result, step, "src");
		Mat humanRes = iiHumanRes.getMat();
		Mat bwHumanRes = Mat.zeros(humanRes.size(), humanRes.type());
		Imgproc.cvtColor(humanRes, bwHumanRes, Imgproc.COLOR_BGR2GRAY);
		bwHumanRes.convertTo(bwHumanRes, CvType.CV_8U);
		this.pictureService.saveResult(bwHumanRes.clone(), result, step, "humanRes");
		Mat hrDT = this.pictureService.distanceTransform(bwHumanRes);
		this.pictureService.saveResult(hrDT.clone(), result, step, "humanRes_dt", 1000);

		Map<String, Double> testResults = new HashMap<>();

		Double colorResult = compareColorMethod(src.clone(), hrDT.clone(), result);
		testResults.put("COLOR,,,", colorResult);

		Double shapeResult = compareShapeMethod(src.clone(), hrDT.clone(), result);
		testResults.put("SHAPE,,,", shapeResult);

		for (Integer depth : this.depths) {
			for (Integer maskSize : this.maskSizes) {
				for (Set<AlgorithmOptions> options : this.optionsList) {
					Double notConnResult = compareNotConnectedMethod(
						src.clone(),
						hrDT.clone(),
						depth,
						maskSize,
						options,
						result
					);
					String optS = options.stream().map(AlgorithmOptions::name).collect(Collectors.joining("-"));
					testResults.put("NC," + depth + "," + maskSize + "," + optS, notConnResult);
				}
			}
		}
		String results = this.formatResults(testResults);
		result.setTestStats(results);
		log.info("results :\n{}", results);

		String maxResult = testResults.entrySet().stream()
			.max(Comparator.comparingDouble(Map.Entry::getValue))
			.map(_e -> "[" + _e.getKey() + ";\t" + _e.getValue() + "]")
			.get();
		log.info("Max similarity = {}", maxResult);
		return result;
	}

	public double compareColorMethod(Mat src, Mat humanRes, ImageInfo result) {
		ImageInfo ii = new ImageInfo();
		ii.setMat(src);
		ii.setResults(new LinkedList<>());
		ii = this.pictureService.colorAutoMarkerWatershed(ii, new HashSet<>(Arrays.asList(AlgorithmOptions.NO_SAVE_STEPS)));
		Mat lastResultMat = CollectionUtil.getLastOf(ii.getResults()).getMat();
		this.pictureService.saveResult(lastResultMat.clone(), result, 0, "color_method");
		Mat dt = this.pictureService.distanceTransform(lastResultMat);
		this.pictureService.saveResult(dt.clone(), result, 0, "color_method_dt", 1000);
		return getSimilarity(dt, humanRes);
	}

	public double compareShapeMethod(Mat src, Mat humanRes, ImageInfo result) {
		ImageInfo ii = new ImageInfo();
		ii.setMat(src);
		ii.setResults(new LinkedList<>());
		ii = this.pictureService.shapeAutoMarkerWatershed(ii, new HashSet<>(Arrays.asList(AlgorithmOptions.NO_SAVE_STEPS)));
		Mat lastResultMat = CollectionUtil.getLastOf(ii.getResults()).getMat();
		this.pictureService.saveResult(lastResultMat.clone(), result, 0, "shape_method");
		Mat dt = this.pictureService.distanceTransform(lastResultMat);
		this.pictureService.saveResult(dt.clone(), result, 0, "shape_method_dt", 1000);
		return getSimilarity(dt, humanRes);
	}

	public double compareNotConnectedMethod(Mat src, Mat humanRes, Integer depth, Integer maskSize, Set<AlgorithmOptions> options, ImageInfo result) {

		ImageInfo ii = new ImageInfo();
		ii.setMat(src);
		ii.setResults(new LinkedList<>());
		ii = this.pictureService.notConnectedMarkers(ii, depth, maskSize, options);
		Mat lastResultMat = CollectionUtil.getLastOf(ii.getResults()).getMat();
		String optS = String.join("_", options.stream().map(AlgorithmOptions::name).collect(Collectors.joining("_")));
		this.pictureService.saveResult(lastResultMat.clone(), result, 0, depth + "_" + maskSize + "_" + optS);
		Mat dt = this.pictureService.distanceTransform(lastResultMat);
		this.pictureService.saveResult(dt.clone(), result, 0, depth + "_" + maskSize + "_" + optS + "_dt", 1000);
		return getSimilarity(dt, humanRes);
	}

	private String formatResults(Map<String, Double> results) {
		StringBuilder formattedResult = new StringBuilder();

		results.forEach((key, value) -> {
			formattedResult.append(String.format("%.4f", value)).append("\t");
			String[] keys = key.split(",");
			Arrays.stream(keys).map(s -> s + "\t").forEach(formattedResult::append);
			formattedResult.append("\n");
		});
		return formattedResult.toString();
	}
}
