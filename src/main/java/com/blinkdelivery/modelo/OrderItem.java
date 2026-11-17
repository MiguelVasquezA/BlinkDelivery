package com.blinkdelivery.modelo;

import java.io.Serializable;
import java.util.Objects;

/**
 * Línea de un {@link Order}: un producto, la cantidad pedida y el precio
 * unitario pactado.
 * <p>
 * Es inmutable y congela el precio unitario al momento de crearse (GRASP
 * Protected Variations): si después cambia el precio del producto en el
 * catálogo, el historial financiero del pedido no se altera.
 * </p>
 *
 * @author Miguel Vasquez
 * @version 2.0
 */
public final class OrderItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Product producto;
    private final int cantidad;
    private final double precioUnitario;

    /**
     * Crea una línea de pedido tomando el precio vigente del producto.
     *
     * @param producto Producto pedido (obligatorio y disponible).
     * @param cantidad Unidades (mayor que cero).
     * @throws IllegalArgumentException Si el producto es nulo o no está disponible, o la cantidad no es positiva.
     */
    public OrderItem(Product producto, int cantidad) {
        this(producto, cantidad, Validaciones.noNulo(producto, "El producto").getPrecio());
        if (!producto.isDisponible()) {
            throw new IllegalArgumentException("El producto '" + producto.getNombre() + "' no está disponible.");
        }
    }

    /**
     * Reconstruye una línea de pedido con un precio histórico ya pactado
     * (por ejemplo, al cargar el pedido desde disco).
     *
     * @param producto       Producto pedido (obligatorio).
     * @param cantidad       Unidades (mayor que cero).
     * @param precioUnitario Precio unitario pactado (no negativo).
     * @throws IllegalArgumentException Si el producto es nulo, la cantidad no es positiva o el precio es negativo.
     */
    public OrderItem(Product producto, int cantidad, double precioUnitario) {
        this.producto = Validaciones.noNulo(producto, "El producto");
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }
        this.cantidad = cantidad;
        this.precioUnitario = Validaciones.noNegativo(precioUnitario, "El precio unitario");
    }

    /** @return Producto pedido. */
    public Product getProducto() {
        return producto;
    }

    /** @return Unidades pedidas. */
    public int getCantidad() {
        return cantidad;
    }

    /** @return Precio unitario congelado al crear la línea. */
    public double getPrecioUnitario() {
        return precioUnitario;
    }

    /** @return Precio unitario por cantidad. */
    public double getSubtotal() {
        return precioUnitario * cantidad;
    }

    /**
     * Dos líneas son iguales si tienen el mismo producto, cantidad y precio
     * (una línea no tiene identificador propio: es un objeto de valor).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OrderItem that = (OrderItem) o;
        return cantidad == that.cantidad
                && Double.compare(precioUnitario, that.precioUnitario) == 0
                && producto.equals(that.producto);
    }

    @Override
    public int hashCode() {
        return Objects.hash(producto, cantidad, precioUnitario);
    }

    @Override
    public String toString() {
        return cantidad + " x " + producto.getNombre() + " @ " + String.format("$%,.0f", precioUnitario)
                + " = " + String.format("$%,.0f", getSubtotal());
    }
}
