package ru.shayhulud.opencvcmsegment.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class WrongMatBodyLengthException extends RuntimeException {

	public WrongMatBodyLengthException(String message) {
		super(message);
	}
}
