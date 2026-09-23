package com.finance_backend.category.exception;

import com.finance_backend.common.exception.ConflictException;
import com.finance_backend.category.entity.CategoryType;

public class DuplicateCategoryException extends ConflictException {

    public DuplicateCategoryException(String name, CategoryType type) {
        super("A category named '" + name + "' already exists for type " + type);
    }
}