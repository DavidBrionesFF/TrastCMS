package com.bytecode.tratcms.repository;

import java.util.List;

import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;

public interface BaseRep<T> {
	public boolean save(T object);
	public boolean update(T object);
	public List<T> findAll(Pageable pageable);
	public T findById(int Id);
}
