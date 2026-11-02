package se.sundsvall.activityfeed.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * @param antal number of activities
 */
@Schema(description = "Antal kundhändelser", accessMode = READ_ONLY)
public record AntalKundhandelser(@Schema(examples = "5") long antal) {
}
