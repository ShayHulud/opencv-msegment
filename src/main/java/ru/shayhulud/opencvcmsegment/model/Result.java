package ru.shayhulud.opencvcmsegment.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.opencv.core.Mat;

/**
 * Объект результата работы алгоритма.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result {

	private Mat mat;
	private int step;
	private String stepName;

	private boolean needToMultiply;
	private Double multiplier;
}
