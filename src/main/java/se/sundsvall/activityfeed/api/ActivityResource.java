package se.sundsvall.activityfeed.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.activityfeed.api.model.AntalKundhandelser;
import se.sundsvall.activityfeed.api.model.CachadKundhandelse;
import se.sundsvall.activityfeed.api.model.NyKundhandelse;
import se.sundsvall.activityfeed.service.ActivityService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

/**
 * Ingest into the activity cache. The business system, or an integration platform, sends activities here when something
 * happens in an errand.
 */
@RestController
@Validated
@RequestMapping("/{municipalityId}/kundhandelser")
@Tag(name = "Ärendecache", description = "Läs in, hämta och ta bort kundhändelser i cachen")
@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
	Problem.class, ConstraintViolationProblem.class
})))
@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
class ActivityResource {

	/** Activities per request. The whole list is saved in one transaction. */
	private static final int MAX_BATCH_SIZE = 1000;

	private final ActivityService activityService;

	ActivityResource(final ActivityService activityService) {
		this.activityService = activityService;
	}

	@PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Lägg till eller ersätt kundhändelser",
		description = "Skicka en lista med högst " + MAX_BATCH_SIZE + " kundhändelser. Befintlig kundhändelse med samma kundhandelseId ersätts. "
			+ "Listan sparas i en transaktion; vid 409 (samtidig inläsning av samma nya kundhandelseId) kan anropet skickas om oförändrat.",
		responses = {
			@ApiResponse(responseCode = "201", description = "Sparat", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "409", description = "Samtidig ändring", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<AntalKundhandelser> sparaKundhandelser(
		@Parameter(name = "municipalityId", description = "Kommun-id", example = "2281") @PathVariable @ValidMunicipalityId final String municipalityId,
		@RequestBody @NotEmpty(message = "listan får inte vara tom") @Size(max = MAX_BATCH_SIZE, message = "högst {max} kundhändelser per anrop") final List<@Valid NyKundhandelse> kundhandelser) {

		return status(CREATED).body(new AntalKundhandelser(activityService.save(municipalityId, kundhandelser)));
	}

	@GetMapping(path = "/{kundhandelseId}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Hämta en kundhändelse ur cachen", responses = @ApiResponse(responseCode = "200", description = "Lyckat anrop", useReturnTypeSchema = true))
	ResponseEntity<CachadKundhandelse> hamtaKundhandelse(
		@Parameter(name = "municipalityId", description = "Kommun-id", example = "2281") @PathVariable @ValidMunicipalityId final String municipalityId,
		@Parameter(name = "kundhandelseId", description = "Kundhändelsens id", example = "REFKOM-BYGG-2026-00123-1") @PathVariable @NotBlank @Size(max = 100) final String kundhandelseId) {

		return ok(activityService.read(municipalityId, kundhandelseId));
	}

	@DeleteMapping(path = "/{kundhandelseId}", produces = ALL_VALUE)
	@Operation(summary = "Ta bort en kundhändelse ur cachen", responses = @ApiResponse(responseCode = "204", description = "Borttagen", useReturnTypeSchema = true))
	ResponseEntity<Void> taBortKundhandelse(
		@Parameter(name = "municipalityId", description = "Kommun-id", example = "2281") @PathVariable @ValidMunicipalityId final String municipalityId,
		@Parameter(name = "kundhandelseId", description = "Kundhändelsens id", example = "REFKOM-BYGG-2026-00123-1") @PathVariable @NotBlank @Size(max = 100) final String kundhandelseId) {

		activityService.delete(municipalityId, kundhandelseId);
		return noContent().build();
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Antal kundhändelser i cachen för kommunen", responses = @ApiResponse(responseCode = "200", description = "Lyckat anrop", useReturnTypeSchema = true))
	ResponseEntity<AntalKundhandelser> antalKundhandelser(
		@Parameter(name = "municipalityId", description = "Kommun-id", example = "2281") @PathVariable @ValidMunicipalityId final String municipalityId) {

		return ok(new AntalKundhandelser(activityService.count(municipalityId)));
	}
}
