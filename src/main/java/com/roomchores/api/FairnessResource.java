package com.roomchores.api;

import com.roomchores.service.AssignmentService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.time.YearMonth;

@Path("/api/fairness")
@Produces(MediaType.APPLICATION_JSON)
public class FairnessResource {

    @Inject
    AssignmentService service;

    /** Per-roommate earned vs fair-share points for a month (default: current month). */
    @GET
    public AssignmentService.FairnessReport report(@QueryParam("month") String month) {
        YearMonth current = YearMonth.from(service.today());
        return service.fairness(ApiDates.month(month, current));
    }
}
