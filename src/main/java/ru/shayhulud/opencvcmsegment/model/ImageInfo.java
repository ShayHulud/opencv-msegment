package ru.shayhulud.opencvcmsegment.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.opencv.core.Mat;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImageInfo {

	private String method;
	private String imageFileName;
	private Mat mat;
	private String outputDirName;
	private List<Result> results;
}
