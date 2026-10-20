package com.cinemaai.identity.service;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface GoogleTokenVerifier {

    GoogleTokenInfo verify(String credential);

    record GoogleTokenInfo(
            @JsonProperty("sub")
            String subject,

            @JsonProperty("aud")
            String audience,

            String email,

            @JsonProperty("email_verified")
            Boolean emailVerified,

            String name,

            @JsonProperty("given_name")
            String givenName,

            @JsonProperty("family_name")
            String familyName,

            String picture
    ) {
    }
}
