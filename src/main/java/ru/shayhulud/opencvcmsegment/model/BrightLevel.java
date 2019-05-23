package ru.shayhulud.opencvcmsegment.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Объект для хранения информации об уровне яркости.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BrightLevel implements Cloneable {
	private int start;
	private int end;
	private int count;

	public BrightLevel clone() {
		return new BrightLevel(
			this.start,
			this.end,
			this.count
		);
	}

	//TODO: Возможно расширить под диапазон.
	public int getMeanLevel() {
		if (this.start == this.end) {
			return this.start;
		}
		int range = this.end - this.start;
		if (range == 1) {
			return this.start;
		}
		int mean = range / 2;
		return this.start + mean;
	}

	public BrightLevel getMeanDiap(int range) {
		BrightLevel meanDiap = new BrightLevel();
		int actualDiametr = this.end - this.start;
		if (actualDiametr <= range * 2) {
			meanDiap.setStart(this.start);
			meanDiap.setEnd(this.end);
			meanDiap.setCount(this.count);
			return meanDiap;
		}

		int mean = this.getMeanLevel();
		int nStart = mean - range < this.start ? mean - range + 1 : mean - range;
		meanDiap.setStart(nStart);
		int nEnd = mean + range > this.end ? mean + range - 1 : mean + range;
		meanDiap.setEnd(nEnd);
		meanDiap.setCount(nEnd - nStart + 1);
		return meanDiap;
	}

	public String getRangeString() {
		return this.start + "-" + this.end;
	}
}
