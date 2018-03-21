package gui.controller;

import java.util.List;

public interface DataHolder<T> {

	public void add(T t);

	public void set(List<T> list);

	public List<T> getData();

	public void clear();

}
