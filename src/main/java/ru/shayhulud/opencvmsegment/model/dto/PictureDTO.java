package ru.shayhulud.opencvmsegment.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Picture object DTO.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PictureDTO implements Serializable {
	private byte[] content;
}
