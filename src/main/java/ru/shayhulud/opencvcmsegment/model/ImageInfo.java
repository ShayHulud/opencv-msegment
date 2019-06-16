package ru.shayhulud.opencvcmsegment.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.opencv.core.Mat;
import ru.shayhulud.opencvcmsegment.common.util.DateUtils;
import ru.shayhulud.opencvcmsegment.model.dic.SegMethod;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Объект информации об изображении.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImageInfo implements Cloneable {

	private SegMethod method;
	private String imageFileName;
	private String imageFullFileName;
	private String imageDir;
	private Mat mat;
	private String outputDirName;
	private List<Result> results;
	private Integer depth;
	private String testStats;

	public ImageInfo clone() {
		ImageInfo _newObj = new ImageInfo();
		_newObj.setImageFileName(this.imageFileName);
		_newObj.setImageFullFileName(this.imageFullFileName);
		_newObj.setMat(this.mat.clone());
		_newObj.setResults(new ArrayList<>());
		_newObj.setOutputDirName(this.outputDirName);
		_newObj.setImageDir(this.imageDir);
		return _newObj;
	}

	public String getOutputDirPath() {
		String folderName = DateUtils.fromTimestampFormatted(new Date().getTime());
		String outputDirPath = this.imageDir + File.separator +
			this.outputDirName + File.separator +
			folderName;
		return outputDirPath;
	}
}
