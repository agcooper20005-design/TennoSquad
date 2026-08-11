package com.aces.tennosquad.exception;

public class DuplicateHostListingException
        extends RuntimeException {

    public DuplicateHostListingException(
            Long hostId,
            Long missionId
    ) {
        super(
                "User " + hostId
                        + " already has an active listing for mission "
                        + missionId
        );
    }
}