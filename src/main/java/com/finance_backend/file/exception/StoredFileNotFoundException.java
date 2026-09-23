package com.finance_backend.file.exception;

import com.finance_backend.common.exception.ResourceNotFoundException;

public class StoredFileNotFoundException extends ResourceNotFoundException {

    public StoredFileNotFoundException(Long id) {
        super("Stored file not found with id: " + id);
    }
}
