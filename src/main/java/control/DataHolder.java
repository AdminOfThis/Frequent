package control;

import java.util.List;

public interface DataHolder<T> {

	/**
	 * Adds a data entry from the instance {@link T} back to the data holding
	 * instance, after loading. Accepts all {@link Object}, the sorting is part of
	 * the implementation
	 * 
	 * @param t
	 */
	public void add(Object t);

	/**
	 * Overwrides all currently existing data with the list of new data
	 * 
	 * @param list The list of new Data
	 */
	public void set(List<T> list);

	/**
	 * 
	 * @return data all the data that should be safed and later restored
	 */
	public List<T> getData();

	/**
	 * Clears all the previously existing data
	 */
	public void clear();

}
