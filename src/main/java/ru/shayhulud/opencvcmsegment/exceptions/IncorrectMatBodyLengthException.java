package ru.shayhulud.opencvcmsegment.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class IncorrectMatBodyLengthException extends RuntimeException {

	public IncorrectMatBodyLengthException(String message) {
		super(message);
	}
}
