package com.example.smartcampus.exception;

import com.example.smartcampus.model.ErrorMessage;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {
    @Override
    public Response toResponse(SensorUnavailableException exception) {
        ErrorMessage error = new ErrorMessage(exception.getMessage(), 403);
        return Response.status(Response.Status.FORBIDDEN).entity(error).build();
    }
}
