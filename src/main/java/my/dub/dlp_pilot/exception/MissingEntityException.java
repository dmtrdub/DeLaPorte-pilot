package my.dub.dlp_pilot.exception;

import lombok.Getter;

/**
 * Exception indicating the absence of an entity sought by a specific criterion.
 */
@Getter
public class MissingEntityException extends RuntimeException {

    private final String entityClassName;

    private final String searchCriteria;

    public MissingEntityException(Class<?> entityClass, String... searchCriteria) {
        super(String.format("%s entity not found by the following search criteria: %s", entityClass.getSimpleName(),
                            String.join(" ; ", searchCriteria)));
        this.entityClassName = entityClass.getSimpleName();
        this.searchCriteria = String.join(" ; ", searchCriteria);
    }
}
