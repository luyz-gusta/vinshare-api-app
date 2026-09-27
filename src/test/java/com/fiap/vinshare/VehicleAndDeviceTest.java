package com.fiap.vinshare;

import com.fiap.vinshare.domain.entities.Customer;
import com.fiap.vinshare.domain.entities.Vehicle;
import com.fiap.vinshare.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VehicleAndDeviceTest extends IntegrationTest {

    @Test
    void odometroNaoPodeRegredir() throws Exception {
        Customer c = fixtures.customer();
        Vehicle v = fixtures.vehicle(c);   // 15.000 km
        mockMvc.perform(patch("/vehicles/" + v.getId() + "/odometer")
                        .header("Authorization", bearer(fixtures.tokenFor(c.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"km\": 1000}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void odometroAvanca() throws Exception {
        Customer c = fixtures.customer();
        Vehicle v = fixtures.vehicle(c);
        mockMvc.perform(patch("/vehicles/" + v.getId() + "/odometer")
                        .header("Authorization", bearer(fixtures.tokenFor(c.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"km\": 20000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentKm").value(20000));
    }

    @Test
    void tokenDePushDeOutroUsuarioRetorna409() throws Exception {
        String pushToken = "ExponentPushToken[" + UUID.randomUUID() + "]";
        String body = "{\"token\":\"%s\",\"platform\":\"ANDROID\"}".formatted(pushToken);

        mockMvc.perform(post("/me/devices")
                        .header("Authorization", bearer(fixtures.tokenFor(fixtures.customer().getUser())))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/me/devices")
                        .header("Authorization", bearer(fixtures.tokenFor(fixtures.customer().getUser())))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void mesmoUsuarioPodeReregistrarOProprioToken() throws Exception {
        String token = fixtures.tokenFor(fixtures.customer().getUser());
        String body = "{\"token\":\"ExponentPushToken[%s]\",\"platform\":\"IOS\"}".formatted(UUID.randomUUID());
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/me/devices")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated());
        }
    }
}
