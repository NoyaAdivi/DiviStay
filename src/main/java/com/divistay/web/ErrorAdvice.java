package com.divistay.web;

import com.divistay.domain.DomainException;
import com.divistay.domain.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Turns errors into a friendly page instead of a stack trace. */
@ControllerAdvice
public class ErrorAdvice {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(NotFoundException e, Model model) {
        model.addAttribute("title", "Not found");
        model.addAttribute("message", e.getMessage());
        return "error";
    }

    @ExceptionHandler(DomainException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String domain(DomainException e, Model model) {
        model.addAttribute("title", "Something isn't right");
        model.addAttribute("message", e.getMessage());
        return "error";
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String badRequest(Exception e, Model model) {
        model.addAttribute("title", "Missing details");
        model.addAttribute("message", "Please choose your dates and try again.");
        return "error";
    }
}
