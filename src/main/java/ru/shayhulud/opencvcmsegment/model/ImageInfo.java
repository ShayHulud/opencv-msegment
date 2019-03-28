package ru.shayhulud.opencvcmsegment.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImageInfo implements Cloneable {

	private String method;
	private String imageFileName;
	private Mat mat;
	private String outputDirName;
	private List<Result> results;
	private Integer depth;

	public ImageInfo clone() {
		ImageInfo _newObj = new ImageInfo();
		_newObj.setImageFileName(this.imageFileName);
		_newObj.setMat(this.mat.clone());
		_newObj.setResults(new ArrayList<>());
		_newObj.setOutputDirName(this.outputDirName);
		return _newObj;
	}
}
