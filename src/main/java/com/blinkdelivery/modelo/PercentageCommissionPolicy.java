package com.blinkdelivery.modelo;

import java.io.Serializable;
import java.util.Objects;

/**
 * Política de comisión basada en un porcentaje fijo sobre el subtotal del pedido.
 * Por defecto aplica el 10 % definido en la regla de negocio RN20.
 *
 * @author Miguel Vasquez
 * @version 2.0
 */
public class PercentageCommissionPolicy implements CommissionPolicy, Serializable {
    private static final long serialVersionUID = 1L;

    /** Tasa de comisión por defecto de la plataforma (10 %). */
    public static final double TASA_POR_DEFECTO = 0.10;

    private final double tasa;

    /** Crea la política con la tasa por defecto del 10 %. */
    public PercentageCommissionPolicy() {
        this(TASA_POR_DEFECTO);
    }

    /**
     * Crea la política con una tasa configurable.
     *
     * @param tasa Tasa expresada como fracción entre 0 y 1 (por ejemplo, 0.10 = 10 %).
     * @throws IllegalArgumentException Si la tasa está fuera del rango [0, 1].
     */
    public PercentageCommissionPolicy(double tasa) {
        if (Double.isNaN(tasa) || tasa < 0 || tasa > 1) {
            throw new IllegalArgumentException("La tasa de comisión debe estar entre 0 y 1 (por ejemplo, 0.10 para 10 %).");
        }
        this.tasa = tasa;
    }

    /** @return Tasa de comisión como fracción. */
    public double getTasa() {
        return tasa;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double calcularComision(double subtotal) {
        Validaciones.noNegativo(subtotal, "El monto sobre el que se calcula la comisión");
        return subtotal * tasa;
    }

    /** Dos políticas porcentuales son iguales si aplican la misma tasa (no tienen id). */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return Double.compare(tasa, ((PercentageCommissionPolicy) o).tasa) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tasa);
    }

    @Override
    public String toString() {
        return String.format("Comisión del %.1f %%", tasa * 100);
    }
}
