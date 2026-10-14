package dev.campusdesk.common;

public class ApiException extends RuntimeException {
    private final int status;
    private final String code;
    public ApiException(int status, String code, String message) {
        super(message); this.status = status; this.code = code;
    }
    public int status() { return status; }
    public String code() { return code; }
    public static ApiException missing(String resource) {
        return new ApiException(404, "NOT_FOUND", resource + " not found");
    }
    public static ApiException conflict() {
        return new ApiException(409, "VERSION_CONFLICT", "The resource changed. Reload it and retry with its current version.");
    }
}
