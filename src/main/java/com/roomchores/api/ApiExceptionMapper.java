package com.roomchores.api;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

/** Makes every thrown BadRequest/NotFound/Conflict come back as {"error": "..."} JSON. */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException exception) {
        String message = exception.getMessage() == null ? "Request failed" : exception.getMessage();
        return Response.status(exception.getResponse().getStatus())
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", message))
                .build();
    }
}
