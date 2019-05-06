package ru.shayhulud.opencvcmsegment.model.dic;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum SegMethod {

	/**
	 * Ручные маркеры.
	 */
	HAND_METHOD("_hand"),
	/**
	 * Маркеры по цвету.
	 */
	COLOR_METHOD("_color"),
	/**
	 * Маркеры по форме.
	 */
	SHAPE_METHOD("_shape"),
	/**
	 * Глубинной яркости.
	 */
	BRIGHT_DEPTH_METHOD("_bright_depth"),
	/**
	 * Несоедиенные маркеры.
	 */
	NOT_CONNECTED_MARKERS("_not_connected");

	@Getter
	private final String fname;
}
