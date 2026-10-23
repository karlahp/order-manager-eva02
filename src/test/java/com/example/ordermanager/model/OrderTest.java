package com.example.ordermanager.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    /** Devuelve cualquier estado distinto de PENDING, sin depender de los nombres del enum. */
    private static OrderStatus estadoNoPendiente() {
        return Arrays.stream(OrderStatus.values())
                .filter(s -> s != OrderStatus.PENDING)
                .findFirst()
                .orElseThrow();
    }

    // ---------- Constructor ----------

    @Test
    void constructor_valido_inicializaEstadoYTotal() {
        Order o = new Order("  Ana  ");

        assertEquals("Ana", o.getCustomerName());
        assertEquals(OrderStatus.PENDING, o.getStatus());
        assertEquals(BigDecimal.ZERO, o.getTotalAmount());
        assertTrue(o.getItems().isEmpty());
    }

    @Test
    void constructor_nombreNulo_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new Order(null));
        assertEquals("El nombre del cliente es obligatorio", ex.getMessage());
    }

    @Test
    void constructor_nombreEnBlanco_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new Order("   "));
    }

    @Test
    void constructorVacio_dejaCamposSinInicializar() {
        Order o = new Order();

        assertNull(o.getId());
        assertNull(o.getCustomerName());
        assertNull(o.getStatus());
        assertNull(o.getTotalAmount());
        assertNull(o.getCreatedAt());
        assertNotNull(o.getItems());
    }

    // ---------- addItem / recalculateTotal ----------

    @Test
    void addItem_unItem_actualizaTotal() {
        Order o = new Order("Ana");
        o.addItem(new OrderItem("A", 2, new BigDecimal("10.00")));

        assertEquals(1, o.getItems().size());
        assertEquals(0, new BigDecimal("20.00").compareTo(o.getTotalAmount()));
    }

    @Test
    void addItem_variosItems_sumaSubtotales() {
        Order o = new Order("Ana");
        o.addItem(new OrderItem("A", 2, new BigDecimal("10.00")));   // 20
        o.addItem(new OrderItem("B", 3, new BigDecimal("1.50")));    // 4.50

        assertEquals(2, o.getItems().size());
        assertEquals(0, new BigDecimal("24.50").compareTo(o.getTotalAmount()));
    }

    // ---------- canBeModified (2 ramas) ----------

    @Test
    void canBeModified_pendiente_true() {
        assertTrue(new Order("Ana").canBeModified());
    }

    @Test
    void canBeModified_otroEstado_false() {
        Order o = new Order("Ana");
        o.setStatus(estadoNoPendiente());
        assertFalse(o.canBeModified());
    }

    // ---------- onCreate (@PrePersist, 4 ramas) ----------

    @Test
    void onCreate_sinEstadoNiTotal_aplicaValoresPorDefecto() {
        Order o = new Order();            // status y totalAmount son null
        LocalDateTime antes = LocalDateTime.now();

        o.onCreate();

        assertNotNull(o.getCreatedAt());
        assertFalse(o.getCreatedAt().isBefore(antes));
        assertEquals(OrderStatus.PENDING, o.getStatus());
        assertEquals(BigDecimal.ZERO, o.getTotalAmount());
    }

    @Test
    void onCreate_conEstadoYTotal_noLosSobrescribe() {
        Order o = new Order();
        OrderStatus estado = estadoNoPendiente();
        o.setStatus(estado);
        o.setTotalAmount(new BigDecimal("99.99"));

        o.onCreate();

        assertEquals(estado, o.getStatus());
        assertEquals(new BigDecimal("99.99"), o.getTotalAmount());
        assertNotNull(o.getCreatedAt());
    }

    // ---------- Getters y setters ----------

    @Test
    void setters_actualizanLosCampos() {
        Order o = new Order();
        LocalDateTime fecha = LocalDateTime.of(2026, 1, 1, 10, 0);
        List<OrderItem> lista = new ArrayList<>();

        o.setId(5L);
        o.setCustomerName("Luis");
        o.setStatus(OrderStatus.PENDING);
        o.setTotalAmount(BigDecimal.TEN);
        o.setCreatedAt(fecha);
        o.setItems(lista);

        assertEquals(5L, o.getId());
        assertEquals("Luis", o.getCustomerName());
        assertEquals(OrderStatus.PENDING, o.getStatus());
        assertEquals(BigDecimal.TEN, o.getTotalAmount());
        assertEquals(fecha, o.getCreatedAt());
        assertSame(lista, o.getItems());
    }

    // ---------- equals / hashCode (6 ramas) ----------

    @Test
    void equals_mismoObjeto_true() {
        Order a = new Order("Ana");
        assertEquals(a, a);
    }

    @Test
    void equals_null_false() {
        assertFalse(new Order("Ana").equals(null));
    }

    @Test
    void equals_otraClase_false() {
        assertNotEquals(new Order("Ana"), "no soy un Order");
    }

    @Test
    void equals_mismoId_trueYMismoHashCode() {
        Order a = new Order("Ana");   a.setId(1L);
        Order b = new Order("Luis");  b.setId(1L);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_distintoId_false() {
        Order a = new Order("Ana");  a.setId(1L);
        Order b = new Order("Ana");  b.setId(2L);

        assertNotEquals(a, b);
    }
}
