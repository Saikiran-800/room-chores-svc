package com.roomchores.api;

import com.roomchores.model.Chore;
import com.roomchores.model.Frequency;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
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
import java.util.List;
import java.util.Locale;

@Path("/api/chores")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChoreResource {

    public record ChoreRequest(
            @NotBlank String code,
            @NotBlank String name,
            @NotBlank String category,
            @Min(1) @Max(10) int points,
            @Min(1) Integer estimatedMinutes,
            Frequency frequency,
            String requiredSkill,
            String location,
            String parentCode,
            Boolean enabled) {
    }

    @GET
    public List<Chore> list(@QueryParam("category") String category,
                            @QueryParam("includeDisabled") boolean includeDisabled) {
        List<Chore> chores = Chore.list("order by category, name");
        return chores.stream()
                .filter(c -> includeDisabled || c.enabled)
                .filter(c -> category == null || category.isBlank() || c.category.equalsIgnoreCase(category))
                .toList();
    }

    @POST
    @Transactional
    public Response create(@Valid ChoreRequest request) {
        String code = normalizeCode(request.code());
        if (Chore.findById(code) != null) {
            throw new ClientErrorException("A chore with code " + code + " already exists", Response.Status.CONFLICT);
        }
        Chore chore = new Chore();
        chore.code = code;
        apply(chore, request);
        chore.persist();
        return Response.created(URI.create("/api/chores/" + code)).entity(chore).build();
    }

    @PUT
    @Path("/{code}")
    @Transactional
    public Chore update(@PathParam("code") String code, @Valid ChoreRequest request) {
        Chore chore = find(code);
        apply(chore, request);
        return chore;
    }

    @POST
    @Path("/{code}/enable")
    @Transactional
    public Chore enable(@PathParam("code") String code) {
        Chore chore = find(code);
        chore.enabled = true;
        return chore;
    }

    @POST
    @Path("/{code}/disable")
    @Transactional
    public Chore disable(@PathParam("code") String code) {
        Chore chore = find(code);
        chore.enabled = false;
        return chore;
    }

    private static Chore find(String code) {
        Chore chore = Chore.findById(normalizeCode(code));
        if (chore == null) {
            throw new NotFoundException("Chore not found");
        }
        return chore;
    }

    private static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
    }

    private static void apply(Chore chore, ChoreRequest request) {
        chore.name = request.name().trim();
        chore.category = request.category().trim();
        chore.points = request.points();
        chore.estimatedMinutes = request.estimatedMinutes() == null ? 10 : request.estimatedMinutes();
        chore.frequency = request.frequency() == null ? Frequency.AS_NEEDED : request.frequency();
        chore.requiredSkill = blankToNull(request.requiredSkill() == null
                ? null : request.requiredSkill().toUpperCase(Locale.ROOT));
        chore.location = blankToNull(request.location());
        chore.parentCode = blankToNull(request.parentCode());
        if (request.enabled() != null) {
            chore.enabled = request.enabled();
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
