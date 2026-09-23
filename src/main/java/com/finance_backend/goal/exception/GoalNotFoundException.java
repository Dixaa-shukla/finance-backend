package com.finance_backend.goal.exception;
import com.finance_backend.common.exception.ResourceNotFoundException;

public class GoalNotFoundException extends ResourceNotFoundException {

    public GoalNotFoundException(Long id) {
        super("Financial goal not found with id: " + id);
    }
}
