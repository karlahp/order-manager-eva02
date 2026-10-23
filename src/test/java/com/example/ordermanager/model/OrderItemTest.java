package com.example.ordermanager.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderItemTest {

    private static final BigDecimal PRECIO = new BigDecimal("10.50");

    // ---------- Constructor: caso válido ----------

    @Test
    void constructor_valido_asignaCamposYRecortaNombre() {
        OrderItem item = new OrderItem("  Teclado  ", 2, PRECIO);

        assertEquals("Teclado", item.getProductName());
        assertEquals(2, item.getQuantity());
        assertEquals(PRECIO, item.getUnitPrice());
    }

    @Test
    void constructor_precioCero_esValido() {
        // Frontera: signum() == 0 NO es negativo
        OrderItem item = new OrderItem("Regalo", 1, BigDecimal.ZERO);
        assertEquals(0, item.getSubtotal().compareTo(BigDecimal.ZERO));
    }

    // ---------- Constructor: validaciones (cada rama del if) ----------

    @Test
    void constructor_nombreNulo_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new OrderItem(null, 1, PRECIO));
        assertEquals("El nombre del producto es obligatorio", ex.getMessage());
    }

    @Test
    void constructor_nombreEnBlanco_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> new OrderItem("   ", 1, PRECIO));
    }

    @Test
    void constructor_cantidadCero_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new OrderItem("A", 0, PRECIO));
        assertEquals("La cantidad debe ser mayor a 0", ex.getMessage());
    }

    @Test
    void constructor_cantidadNegativa_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> new OrderItem("A", -5, PRECIO));
    }

    @Test
    void constructor_precioNulo_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new OrderItem("A", 1, null));
        assertEquals("El precio unitario no puede ser negativo", ex.getMessage());
    }

    @Test
    void constructor_precioNegativo_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> new OrderItem("A", 1, new BigDecimal("-0.01")));
    }

    // ---------- getSubtotal ----------

    @Test
    void getSubtotal_multiplicaPrecioPorCantidad() {
        OrderItem item = new OrderItem("A", 3, PRECIO);
        assertEquals(0, new BigDecimal("31.50").compareTo(item.getSubtotal()));
    }

    // ---------- Constructor vacío y setters ----------

    @Test
    void constructorVacio_dejaValoresPorDefecto() {
        OrderItem item = new OrderItem();

        assertNull(item.getProductName());
        assertEquals(0, item.getQuantity());
        assertNull(item.getUnitPrice());
    }

    @Test
    void setters_actualizanLosCampos() {
        OrderItem item = new OrderItem();

        item.setProductName("Mouse");
        item.setQuantity(4);
        item.setUnitPrice(new BigDecimal("5.00"));

        assertEquals("Mouse", item.getProductName());
        assertEquals(4, item.getQuantity());
        assertEquals(new BigDecimal("5.00"), item.getUnitPrice());
    }

    // ---------- equals / hashCode (12 ramas) ----------

    @Test
    void equals_mismoObjeto_true() {
        OrderItem a = new OrderItem("A", 1, PRECIO);
        assertEquals(a, a);
    }

    @Test
    void equals_null_false() {
        assertFalse(new OrderItem("A", 1, PRECIO).equals(null));
    }

    @Test
    void equals_otraClase_false() {
        assertNotEquals(new OrderItem("A", 1, PRECIO), "no soy un OrderItem");
    }

    @Test
    void equals_todosLosCamposIguales_trueYMismoHashCode() {
        OrderItem a = new OrderItem("A", 2, PRECIO);
        OrderItem b = new OrderItem("A", 2, new BigDecimal("10.50"));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_cantidadDistinta_false() {
        assertNotEquals(new OrderItem("A", 1, PRECIO), new OrderItem("A", 2, PRECIO));
    }

    @Test
    void equals_nombreDistinto_false() {
        assertNotEquals(new OrderItem("A", 1, PRECIO), new OrderItem("B", 1, PRECIO));
    }

    @Test
    void equals_precioDistinto_false() {
        assertNotEquals(new OrderItem("A", 1, PRECIO),
                new OrderItem("A", 1, new BigDecimal("99.00")));
    }
}
