package my.dub.dlp_pilot.exception;

import lombok.Getter;

@Getter
public class MissingEntityException extends RuntimeException {

    private final String entityClassName;

    private final String searchCriteria;

    public MissingEntityException(String entityClassName, String searchCriteria) {
        super(String.format("%s entity not found by the following search criteria: %s", entityClassName,
                            searchCriteria));
        this.entityClassName = entityClassName;
        this.searchCriteria = searchCriteria;
    }
}
