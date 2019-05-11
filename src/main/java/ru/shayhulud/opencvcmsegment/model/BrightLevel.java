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
}
