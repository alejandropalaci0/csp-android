package com.apasoft.csp.model;

/**
 * Representa un tipo de corte definido por el usuario.
 *
 * <p>Un tipo de corte agrupa la longitud de una pieza y la cantidad de unidades
 * de esa pieza que se necesitan obtener. Es el dato de entrada principal del solver.</p>
 */
public class CutType {

    /** Longitud de la pieza en las unidades de trabajo (mm, cm, etc.). */
    private double length;

    /** Número de piezas de esta longitud que deben obtenerse. */
    private int quantity;

    /**
     * Construye un nuevo tipo de corte.
     *
     * @param length   longitud de la pieza (debe ser &gt; 0)
     * @param quantity número de piezas requeridas (debe ser &gt; 0)
     */
    public CutType(double length, int quantity) {
        this.length = length;
        this.quantity = quantity;
    }

    /**
     * Devuelve la longitud de la pieza.
     *
     * @return longitud en las unidades de trabajo
     */
    public double getLength() {
        return length;
    }

    /**
     * Establece la longitud de la pieza.
     *
     * @param length nueva longitud (debe ser &gt; 0)
     */
    public void setlength(double length) {
        this.length = length;
    }

    /**
     * Devuelve la cantidad de piezas requeridas de este tipo.
     *
     * @return número de piezas
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Establece la cantidad de piezas requeridas de este tipo.
     *
     * @param quantity número de piezas (debe ser &gt; 0)
     */
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}