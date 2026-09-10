package com.prabhupada.festival;

/** Sends one message to everyone on the roster. */
public interface FestivalMailer {

    /**
     * @return how many people were written to, and how much of the day's sending
     * allowance is left.
     */
    Result sendToEveryone(String subject, String body);

    record Result(int sent, int remainingQuota) {
    }

    /** Thrown when the message could not be sent. */
    class SendFailedException extends RuntimeException {
        public SendFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
