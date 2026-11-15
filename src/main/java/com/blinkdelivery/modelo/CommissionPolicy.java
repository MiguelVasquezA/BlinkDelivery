package com.blinkdelivery.modelo;

import java.io.Serializable;

/**
 * Estrategia de cálculo de la comisión que cobra la plataforma por un pedido
 * (patrón Strategy). Permite cambiar la forma de cobro sin modificar {@link Order}.
 * <p>
 * Extiende {@link Serializable} porque cada pedido conserva la política con la
 * que fue liquidado y debe poder persistirse junto con él.
 * </p>
 *
 * @author Miguel Vasquez
 * @version 2.0
 */
public interface CommissionPolicy extends Serializable {

    /**
     * Calcula la comisión de la plataforma sobre un monto.
     *
     * @param subtotal Monto base del pedido (no negativo).
     * @return Valor de la comisión.
     * @throws IllegalArgumentException Si el monto es negativo.
     */
    double calcularComision(double subtotal);
}
