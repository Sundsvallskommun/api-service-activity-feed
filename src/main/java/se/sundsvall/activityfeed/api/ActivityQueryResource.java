package se.sundsvall.activityfeed.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.activityfeed.api.model.ErrorResponse;
import se.sundsvall.activityfeed.api.model.KundhandelseFragaRequest;
import se.sundsvall.activityfeed.api.model.KundhandelseFragaResponse;
import se.sundsvall.activityfeed.configuration.CorrelationIdFilter;
import se.sundsvall.activityfeed.service.ActivityQueryService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;

import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;

/**
 * The producer's interface towards the forwarding service. Same operation, request and response as in Skatteverket's
 * API definition for Mina ärenden kundhändelser. Error responses have the format {"message": "..."}, see
 * {@link ActivityQueryExceptionHandler}.
 */
@RestController
@Validated
@RequestMapping("/{municipalityId}/kundhandelseFragaSynkron")
@Tag(name = "Kundhändelser", description = "Fråga om kundhändelser (Mina ärendens standard)")
class ActivityQueryResource {

	private final ActivityQueryService activityQueryService;

	ActivityQueryResource(final ActivityQueryService activityQueryService) {
		this.activityQueryService = activityQueryService;
	}

	@PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(operationId = "kundhandelseFragaSynkron",
		summary = "Hämta kundhändelser för en eller flera parter (synkron fråga)",
		description = "Besvarar frågan ur ärendecachen. Parter kan anges som kund, ärende eller båda. Filtrera med kundhandelseTyper "
			+ "(prefix matchar underliggande typer), taggar och datumintervall. Sortering och paginering styrs med behandling; "
			+ "paginering kräver sortering.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Lyckat anrop", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "400", description = "Frågan är inte korrekt ställd", content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "404", description = "Kommunen är inte konfigurerad som producent", content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "500", description = "Internt fel", content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
		})
	ResponseEntity<KundhandelseFragaResponse> kundhandelseFragaSynkron(
		@Parameter(name = "municipalityId", description = "Kommun-id", example = "2281") @PathVariable @ValidMunicipalityId final String municipalityId,
		@Parameter(in = ParameterIn.HEADER,
			name = CorrelationIdFilter.HEADER,
			required = true,
			description = "Unikt id per anrop som följer anropskedjan från konsument via Skatteverket. Tjänstebeskrivningen anger UUID; "
				+ "API-definitionen tillåter valfritt format. Här krävs skrivbara ASCII-tecken, högst " + CorrelationIdFilter.MAX_LENGTH + " tecken.",
			example = "0002aa29-49f2-4baf-be51-c7c39c9824b4") @RequestHeader(CorrelationIdFilter.HEADER) @Pattern(regexp = CorrelationIdFilter.FORMAT,
				message = "måste vara en icke-tom sträng med högst " + CorrelationIdFilter.MAX_LENGTH + " skrivbara tecken") final String correlationId,
		@Parameter(in = ParameterIn.HEADER, name = ACCEPT_LANGUAGE, description = "Önskat språk", example = "sv") @RequestHeader(value = ACCEPT_LANGUAGE, required = false) final String acceptLanguage,
		@Valid @NotNull @RequestBody final KundhandelseFragaRequest request) {

		return ok(activityQueryService.answer(municipalityId, request, acceptLanguage));
	}
}
