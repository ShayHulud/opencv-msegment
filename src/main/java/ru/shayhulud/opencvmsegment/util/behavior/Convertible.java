package ru.shayhulud.opencvmsegment.util.behavior;

/**
 * Determine entity ability to convert into DTO type of T.
 *
 * @param <T> DTO type
 */
public interface Convertible<T> {

	T convertToDTO();

}
