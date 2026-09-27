package com.fiap.vinshare.infra.errors;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

import java.net.URI;

/** Fábrica única de ProblemDetail (RFC 7807), usada pelo handler global e pelos filtros. */
public final class ProblemDetails {

    public static final URI TYPE_BASE = URI.create("https://api.fordvinshare.fiap/errors/");

    private ProblemDetails() {}

    public static ProblemDetail of(HttpStatusCode status, String title, String detail, String instance) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(TYPE_BASE.resolve(String.valueOf(status.value())));
        pd.setInstance(URI.create(instance));
        return pd;
    }
}
