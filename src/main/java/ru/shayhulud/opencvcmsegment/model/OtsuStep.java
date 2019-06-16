package ru.shayhulud.opencvcmsegment.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Результат шага рекурсии для Мульти Оцу.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtsuStep {

	private Double maxVar;
	private Integer idxMaxVar;
	private List<Integer> deepers;
}
