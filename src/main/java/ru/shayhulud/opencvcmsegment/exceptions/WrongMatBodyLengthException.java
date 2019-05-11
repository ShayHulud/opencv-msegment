package ru.shayhulud.opencvcmsegment.exceptions;

import lombok.NoArgsConstructor;

/**
 * Ошибка неправильной длинны тела матрицы.
 */
@NoArgsConstructor
public class WrongMatBodyLengthException extends RuntimeException {

	public WrongMatBodyLengthException(String message) {
		super(message);
	}
}
