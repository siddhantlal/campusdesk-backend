package dev.campusdesk.common;

import java.net.URI;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ProblemDetail;

public final class Problems {
    private Problems() {}
    public static ProblemDetail create(int status, String code, String detail, HttpServletRequest request, Map<String, String> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(org.springframework.http.HttpStatusCode.valueOf(status), detail);
        problem.setType(URI.create("urn:campusdesk:error:" + code.toLowerCase(java.util.Locale.ROOT)));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        problem.setProperty("correlationId", request.getAttribute("correlationId"));
        if (!errors.isEmpty()) problem.setProperty("fieldErrors", errors);
        return problem;
    }
}
