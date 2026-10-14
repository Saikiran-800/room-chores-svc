package com.roomchores.api;

import com.roomchores.model.Assignment;
import com.roomchores.model.AssignmentStatus;
import com.roomchores.model.Chore;
import com.roomchores.model.Roommate;
import com.roomchores.service.AssignmentService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Path("/api/assignments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AssignmentResource {

    public record CreateRequest(@NotBlank String choreCode, @NotNull UUID roommateId, String dueDate) {
    }

    public record SwapRequest(@NotNull UUID roommateId) {
    }

    @Inject
    AssignmentService service;

    @GET
    public List<Assignment> list(@QueryParam("from") String from,
                                 @QueryParam("to") String to,
                                 @QueryParam("roommateId") UUID roommateId,
                                 @QueryParam("status") AssignmentStatus status) {
        LocalDate today = service.today();
        LocalDate fromDate = ApiDates.date(from, today.minusDays(7));
        LocalDate toDate = ApiDates.date(to, today.plusDays(7));
        List<Assignment> assignments = Assignment.list(
                "dueDate >= ?1 and dueDate <= ?2 order by dueDate, chore.name", fromDate, toDate);
        return assignments.stream()
                .filter(a -> roommateId == null || a.roommate.id.equals(roommateId))
                .filter(a -> status == null || a.status == status)
                .toList();
    }

    @POST
    @Transactional
    public Response create(@Valid CreateRequest request) {
        Chore chore = Chore.findById(request.choreCode().trim().toUpperCase(Locale.ROOT));
        if (chore == null) {
            throw new NotFoundException("Chore not found");
        }
        Roommate roommate = Roommate.findById(request.roommateId());
        if (roommate == null) {
            throw new NotFoundException("Roommate not found");
        }
        requireEligible(roommate, chore);

        Assignment assignment = new Assignment();
        assignment.chore = chore;
        assignment.roommate = roommate;
        assignment.dueDate = ApiDates.date(request.dueDate(), service.today());
        assignment.persist();
        return Response.created(URI.create("/api/assignments/" + assignment.id)).entity(assignment).build();
    }

    /** Auto-assign everything that is due on the given date (default: today). */
    @POST
    @Path("/generate")
    public AssignmentService.GenerateResult generate(@QueryParam("date") String date) {
        return service.generate(ApiDates.date(date, service.today()));
    }

    @POST
    @Path("/{id}/complete")
    @Transactional
    public Assignment complete(@PathParam("id") UUID id) {
        Assignment assignment = pending(id);
        assignment.status = AssignmentStatus.COMPLETED;
        assignment.pointsAwarded = assignment.chore.points;
        assignment.completedAt = Instant.now();
        return assignment;
    }

    @POST
    @Path("/{id}/miss")
    @Transactional
    public Assignment miss(@PathParam("id") UUID id) {
        Assignment assignment = pending(id);
        assignment.status = AssignmentStatus.MISSED;
        assignment.pointsAwarded = 0;
        return assignment;
    }

    @POST
    @Path("/{id}/swap")
    @Transactional
    public Assignment swap(@PathParam("id") UUID id, @Valid SwapRequest request) {
        Assignment assignment = pending(id);
        Roommate target = Roommate.findById(request.roommateId());
        if (target == null) {
            throw new NotFoundException("Roommate not found");
        }
        requireEligible(target, assignment.chore);
        assignment.note = "Swapped from " + assignment.roommate.name;
        assignment.roommate = target;
        return assignment;
    }

    private static Assignment pending(UUID id) {
        Assignment assignment = Assignment.findById(id);
        if (assignment == null) {
            throw new NotFoundException("Assignment not found");
        }
        if (assignment.status != AssignmentStatus.PENDING) {
            throw new ClientErrorException("Assignment is already " + assignment.status, Response.Status.CONFLICT);
        }
        return assignment;
    }

    private static void requireEligible(Roommate roommate, Chore chore) {
        if (!AssignmentService.isEligible(roommate, chore)) {
            throw new BadRequestException(roommate.name + " is not eligible for " + chore.name
                    + (chore.requiredSkill == null ? "" : " (needs " + chore.requiredSkill + ")"));
        }
    }
}
