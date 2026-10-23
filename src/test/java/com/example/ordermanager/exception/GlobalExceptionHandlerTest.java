package com.example.ordermanager.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FakeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void orderNotFound_devuelve404() throws Exception {
        mockMvc.perform(get("/fake/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void invalidState_devuelve409() throws Exception {
        mockMvc.perform(get("/fake/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("estado inválido"));
    }

    @Test
    void illegalArgument_devuelve400() throws Exception {
        mockMvc.perform(get("/fake/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("dato inválido"));
    }

    @Test
    void validacionFallida_devuelve400ConCampos() throws Exception {
        mockMvc.perform(post("/fake/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error de validación en los datos enviados"))
                .andExpect(jsonPath("$.fields.name").exists());
    }

    @Test
    void excepcionGenerica_devuelve500SinExponerDetalles() throws Exception {
        mockMvc.perform(get("/fake/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("Error interno del servidor. Contacte al administrador."))
                .andExpect(jsonPath("$.message", not(containsString("secreto"))));
    }

    @RestController
    static class FakeController {

        record Body(@NotBlank String name) {}

        @GetMapping("/fake/not-found")
        void notFound() { throw new OrderNotFoundException(99L); }

        @GetMapping("/fake/conflict")
        void conflict() { throw new InvalidOrderStateException("estado inválido"); }

        @GetMapping("/fake/bad-request")
        void badRequest() { throw new IllegalArgumentException("dato inválido"); }

        @PostMapping("/fake/validate")
        void validate(@Valid @RequestBody Body body) { }

        @GetMapping("/fake/boom")
        void boom() { throw new RuntimeException("detalle secreto interno"); }
    }
}
