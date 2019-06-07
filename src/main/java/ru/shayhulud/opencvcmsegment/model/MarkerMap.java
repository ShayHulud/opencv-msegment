package ru.shayhulud.opencvcmsegment.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.opencv.core.Mat;
import org.opencv.core.Size;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Объект маркерной карты.
 */
@Data
@AllArgsConstructor
public class MarkerMap {
	private int idx;
	private BrightLevel brightLevel;
	private Mat allLevel;
	private int allLevelCount;
	private Mat meanLevel;
	private int meanLevelCount;
	private Mat marker;
	private int markerCount;
	private Duration markerDuration;

	public MarkerMap(int idx, BrightLevel brightLevel, Size srcSize, int srcType) {
		this.idx = idx;
		this.brightLevel = brightLevel;
		this.allLevel = new Mat(srcSize, srcType);
		this.allLevelCount = 0;
		this.meanLevel = new Mat(srcSize, srcType);
		this.meanLevelCount = 0;
		this.markerCount = 0;
	}

	public int incrementAllLevelCount() {
		this.allLevelCount = this.allLevelCount + 1;
		return this.allLevelCount;
	}

	public int incrementMeanLevelCount() {
		this.meanLevelCount = this.meanLevelCount + 1;
		return this.meanLevelCount;
	}

	public int incrementMarkerLevelCount() {
		this.markerCount = this.markerCount + 1;
		return this.markerCount;
	}

	public String stats() {
		String sb = "\n" + "[_idx = " + this.idx + "]\n" +
			"[bright level = {" + this.brightLevel.getStart() + "-" + this.brightLevel.getEnd() + "}]\n" +
			"[allLevelCount = " + this.allLevelCount + "; meanLevelCount = " + this.meanLevelCount + "; markerCount = " + this.markerCount + ";]\n" +
			"[markerDuration = formatted:{" +
			this.markerDuration.get(ChronoUnit.SECONDS) / 60 + " min " +
			this.markerDuration.get(ChronoUnit.SECONDS) % 60 + " sec " +
			this.markerDuration.get(ChronoUnit.NANOS) / 1000000 + " mills}; " +
			"allmills:{" + (this.markerDuration.getSeconds() * 1000 + this.markerDuration.getNano() / 1000000) +
			"}]";
		return sb;
	}

}
