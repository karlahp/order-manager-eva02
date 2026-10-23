package com.example.ordermanager.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración: levanta el contexto completo de Spring Boot
 * (controller + service + repositorio JPA + H2) y recorre el flujo crítico
 * del negocio de punta a punta. Cubre los casos críticos exigidos por la
 * política de calidad (IE2).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Flujo completo de pedidos - Integración")
class OrderFlowIntegrationTest {

    private static final String ITEM_JSON =
            "{\"productName\":\"Teclado\",\"quantity\":2,\"unitPrice\":15000.50}";

    @Autowired
    private MockMvc mockMvc;

    private long createOrder() throws Exception {
        String response = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"Ana\",\"items\":[" + ITEM_JSON + "]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    @Test
    @DisplayName("El contexto levanta y /actuator/health responde UP")
    void contextLoadsAndHealthIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("/actuator/info expone el commit para trazabilidad")
    void infoExposesCommit() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.commit").exists());
    }

    @Test
    @DisplayName("Ciclo de vida: PENDING -> CONFIRMED -> SHIPPED -> DELIVERED")
    void fullLifecycle() throws Exception {
        long id = createOrder();

        mockMvc.perform(post("/api/orders/" + id + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\"Mouse\",\"quantity\":1,\"unitPrice\":8000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2));

        mockMvc.perform(put("/api/orders/" + id + "/confirm"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));
        mockMvc.perform(put("/api/orders/" + id + "/ship"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SHIPPED"));
        mockMvc.perform(put("/api/orders/" + id + "/deliver"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DELIVERED"));

        mockMvc.perform(get("/api/orders/status/DELIVERED"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Reglas de negocio: transiciones inválidas devuelven 409")
    void invalidTransitionsReturnConflict() throws Exception {
        long id = createOrder();

        // No se puede enviar un pedido que aún no está confirmado
        mockMvc.perform(put("/api/orders/" + id + "/ship")).andExpect(status().isConflict());

        mockMvc.perform(put("/api/orders/" + id + "/confirm")).andExpect(status().isOk());

        // Un pedido confirmado ya no admite nuevos ítems
        mockMvc.perform(post("/api/orders/" + id + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ITEM_JSON))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/orders/" + id + "/ship")).andExpect(status().isOk());
        mockMvc.perform(put("/api/orders/" + id + "/deliver")).andExpect(status().isOk());

        // Un pedido entregado no puede cancelarse
        mockMvc.perform(put("/api/orders/" + id + "/cancel")).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Cancelar un pedido pendiente y eliminarlo")
    void cancelAndDelete() throws Exception {
        long id = createOrder();

        mockMvc.perform(put("/api/orders/" + id + "/cancel"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(delete("/api/orders/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/orders/" + id)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Validaciones: datos inválidos devuelven 400 y pedido inexistente 404")
    void validationAndNotFound() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"\",\"items\":[]}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"Ana\",\"items\":[{\"productName\":\"X\",\"quantity\":0,\"unitPrice\":10}]}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/orders/999999")).andExpect(status().isNotFound());
    }
}
