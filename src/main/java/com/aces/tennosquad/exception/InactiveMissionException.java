package com.aces.tennosquad.exception;

public class InactiveMissionException
        extends RuntimeException {

    public InactiveMissionException(Long missionId) {
        super(
                "Cannot create a host listing for inactive mission: "
                        + missionId
        );
    }
}