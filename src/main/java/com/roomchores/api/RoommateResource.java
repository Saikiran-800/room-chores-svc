package com.roomchores.api;

import com.roomchores.model.Roommate;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Path("/api/roommates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoommateResource {

    public record RoommateRequest(
            @NotBlank String name,
            @Min(0) @Max(100) Integer capacityPercent,
            Set<String> skills,
            Boolean active) {
    }

    @GET
    public List<Roommate> list(@QueryParam("includeInactive") boolean includeInactive) {
        return includeInactive
                ? Roommate.list("order by name")
                : Roommate.list("active = true order by name");
    }

    @POST
    @Transactional
    public Response create(@Valid RoommateRequest request) {
        Roommate roommate = new Roommate();
        apply(roommate, request);
        roommate.persist();
        return Response.created(URI.create("/api/roommates/" + roommate.id)).entity(roommate).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Roommate update(@PathParam("id") UUID id, @Valid RoommateRequest request) {
        Roommate roommate = Roommate.findById(id);
        if (roommate == null) {
            throw new NotFoundException("Roommate not found");
        }
        apply(roommate, request);
        return roommate;
    }

    /** Soft delete: past assignments keep pointing at the roommate so history stays intact. */
    @DELETE
    @Path("/{id}")
    @Transactional
    public Response deactivate(@PathParam("id") UUID id) {
        Roommate roommate = Roommate.findById(id);
        if (roommate == null) {
            throw new NotFoundException("Roommate not found");
        }
        roommate.active = false;
        return Response.noContent().build();
    }

    private static void apply(Roommate roommate, RoommateRequest request) {
        roommate.name = request.name().trim();
        roommate.capacityPercent = request.capacityPercent() == null ? 100 : request.capacityPercent();
        if (request.active() != null) {
            roommate.active = request.active();
        }
        Set<String> skills = new HashSet<>();
        if (request.skills() != null) {
            for (String skill : request.skills()) {
                if (skill != null && !skill.isBlank()) {
                    skills.add(skill.trim().toUpperCase(Locale.ROOT));
                }
            }
        }
        roommate.skills.clear();
        roommate.skills.addAll(skills);
    }
}
