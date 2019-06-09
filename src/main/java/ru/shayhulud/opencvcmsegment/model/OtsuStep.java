package ru.shayhulud.opencvcmsegment.model;

import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
public class OtsuStep {

	private List<OtsuStep> deepers;
	private Integer idxMaxVar;
	private Double maxVar;

	public OtsuStep() {
		this.deepers = new LinkedList<>();
	}
}
