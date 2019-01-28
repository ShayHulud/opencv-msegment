package ru.shayhulud.opencvmsegment.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.shayhulud.opencvmsegment.model.dto.PictureDTO;
import ru.shayhulud.opencvmsegment.util.behavior.Convertible;

/**
 * Picture object - aggregation of opencv and inner params.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Picture implements Convertible<PictureDTO> {
	private byte[] content;

	@Override
	public PictureDTO convertToDTO() {
		return new PictureDTO(this.content);
	}
}
