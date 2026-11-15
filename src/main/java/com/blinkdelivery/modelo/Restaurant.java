package com.blinkdelivery.modelo;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Restaurante aliado de BlinkDelivery. Es dueño de su catálogo (categorías y
 * productos) y de sus calificaciones, por lo que es el experto en calcular su
 * promedio de calificación (GRASP Information Expert).
 *
 * @author Miguel Vasquez
 * @version 2.0
 */
public class Restaurant implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Valor usado para datos que aún no se han registrado (registros heredados). */
    public static final String POR_DEFINIR = "Por definir";
    /** Hora de apertura por defecto. */
    public static final LocalTime APERTURA_POR_DEFECTO = LocalTime.of(8, 0);
    /** Hora de cierre por defecto. */
    public static final LocalTime CIERRE_POR_DEFECTO = LocalTime.of(22, 0);

    private final String id;
    private String nombre;
    private String tipoCocina;
    private String direccion;
    private String telefono;
    private LocalTime horaApertura;
    private LocalTime horaCierre;
    private RestaurantStatus estado;
    private final ArrayList<Category> categorias = new ArrayList<>();
    private final ArrayList<Product> productos = new ArrayList<>();
    private final ArrayList<Rating> calificaciones = new ArrayList<>();
    private double promedioCalificacion;

    /**
     * Crea un restaurante abierto, sin catálogo ni calificaciones.
     *
     * @param id           Identificador único (obligatorio).
     * @param nombre       Nombre comercial (obligatorio).
     * @param tipoCocina   Tipo de comida que ofrece, por ejemplo "Comida asiática" (obligatorio).
     * @param direccion    Dirección del local (obligatoria).
     * @param telefono     Teléfono del local (obligatorio).
     * @param horaApertura Hora de apertura (obligatoria).
     * @param horaCierre   Hora de cierre (obligatoria y distinta de la apertura).
     * @throws IllegalArgumentException Si algún dato es nulo, vacío o el horario es inválido.
     */
    public Restaurant(String id, String nombre, String tipoCocina, String direccion, String telefono,
                      LocalTime horaApertura, LocalTime horaCierre) {
        this.id = Validaciones.texto(id, "El id del restaurante");
        setNombre(nombre);
        setTipoCocina(tipoCocina);
        setDireccion(direccion);
        setTelefono(telefono);
        setHorario(horaApertura, horaCierre);
        this.estado = RestaurantStatus.ABIERTO;
        this.promedioCalificacion = 0.0;
    }

    /**
     * Constructor de compatibilidad usado por la persistencia en archivo plano:
     * dirección y teléfono quedan como {@value #POR_DEFINIR} y el horario por defecto
     * es de 08:00 a 22:00.
     *
     * @param id       Identificador único.
     * @param name     Nombre comercial.
     * @param category Tipo de cocina.
     * @throws IllegalArgumentException Si algún dato es nulo o vacío.
     */
    public Restaurant(String id, String name, String category) {
        this(id, name, category, POR_DEFINIR, POR_DEFINIR, APERTURA_POR_DEFECTO, CIERRE_POR_DEFECTO);
    }

    // ---- Datos básicos ----

    /** @return Identificador del restaurante. */
    public String getId() {
        return id;
    }

    /** @return Nombre comercial. */
    public String getNombre() {
        return nombre;
    }

    /**
     * @param nombre Nuevo nombre.
     * @throws IllegalArgumentException Si es nulo o vacío.
     */
    public final void setNombre(String nombre) {
        this.nombre = Validaciones.texto(nombre, "El nombre del restaurante");
    }

    /** @return Tipo de cocina que ofrece el restaurante. */
    public String getTipoCocina() {
        return tipoCocina;
    }

    /**
     * @param tipoCocina Nuevo tipo de cocina.
     * @throws IllegalArgumentException Si es nulo o vacío.
     */
    public final void setTipoCocina(String tipoCocina) {
        this.tipoCocina = Validaciones.texto(tipoCocina, "El tipo de cocina");
    }

    /** @return Dirección del local. */
    public String getDireccion() {
        return direccion;
    }

    /**
     * @param direccion Nueva dirección.
     * @throws IllegalArgumentException Si es nula o vacía.
     */
    public final void setDireccion(String direccion) {
        this.direccion = Validaciones.texto(direccion, "La dirección del restaurante");
    }

    /** @return Teléfono del local. */
    public String getTelefono() {
        return telefono;
    }

    /**
     * @param telefono Nuevo teléfono.
     * @throws IllegalArgumentException Si es nulo o vacío.
     */
    public final void setTelefono(String telefono) {
        this.telefono = Validaciones.texto(telefono, "El teléfono del restaurante");
    }

    /** @return Hora de apertura. */
    public LocalTime getHoraApertura() {
        return horaApertura;
    }

    /** @return Hora de cierre. */
    public LocalTime getHoraCierre() {
        return horaCierre;
    }

    /**
     * Define el horario de atención. Si el cierre es anterior a la apertura se
     * interpreta que el restaurante cierra después de medianoche.
     *
     * @param horaApertura Hora de apertura.
     * @param horaCierre   Hora de cierre.
     * @throws IllegalArgumentException Si alguna es nula o ambas son iguales.
     */
    public final void setHorario(LocalTime horaApertura, LocalTime horaCierre) {
        Validaciones.noNulo(horaApertura, "La hora de apertura");
        Validaciones.noNulo(horaCierre, "La hora de cierre");
        if (horaApertura.equals(horaCierre)) {
            throw new IllegalArgumentException("La hora de apertura y la de cierre no pueden ser iguales.");
        }
        this.horaApertura = horaApertura;
        this.horaCierre = horaCierre;
    }

    /**
     * @param hora Hora a consultar.
     * @return {@code true} si la hora está dentro del horario de atención.
     * @throws IllegalArgumentException Si la hora es nula.
     */
    public boolean estaEnHorario(LocalTime hora) {
        Validaciones.noNulo(hora, "La hora a consultar");
        if (horaApertura.isBefore(horaCierre)) {
            return !hora.isBefore(horaApertura) && hora.isBefore(horaCierre);
        }
        return !hora.isBefore(horaApertura) || hora.isBefore(horaCierre);
    }

    /** @return Estado de operación. */
    public RestaurantStatus getEstado() {
        return estado;
    }

    /**
     * @param estado Nuevo estado.
     * @throws IllegalArgumentException Si es nulo.
     */
    public final void setEstado(RestaurantStatus estado) {
        this.estado = Validaciones.noNulo(estado, "El estado del restaurante");
    }

    /** @return {@code true} si el restaurante está {@link RestaurantStatus#ABIERTO}. */
    public boolean estaAbierto() {
        return estado == RestaurantStatus.ABIERTO;
    }

    // ---- Catálogo ----

    /** @return Vista de solo lectura de las categorías del menú. */
    public List<Category> getCategorias() {
        return Collections.unmodifiableList(categorias);
    }

    /**
     * Agrega una categoría al menú.
     *
     * @param categoria Categoría a agregar.
     * @throws IllegalArgumentException Si es nula o ya existe.
     */
    public void agregarCategoria(Category categoria) {
        Validaciones.noNulo(categoria, "La categoría");
        if (categorias.contains(categoria)) {
            throw new IllegalArgumentException("La categoría '" + categoria.getNombre() + "' ya existe en el restaurante.");
        }
        categorias.add(categoria);
    }

    /** @return Vista de solo lectura de los productos del menú. */
    public List<Product> getProductos() {
        return Collections.unmodifiableList(productos);
    }

    /** @return Productos que actualmente se pueden pedir. */
    public List<Product> getProductosDisponibles() {
        List<Product> disponibles = new ArrayList<>();
        for (Product p : productos) {
            if (p.isDisponible()) {
                disponibles.add(p);
            }
        }
        return disponibles;
    }

    /**
     * Agrega un producto al menú. Si el producto trae una categoría que el
     * restaurante aún no tiene, la registra automáticamente.
     *
     * @param producto Producto a agregar.
     * @throws IllegalArgumentException Si es nulo, pertenece a otro restaurante o ya existe.
     */
    public void agregarProducto(Product producto) {
        Validaciones.noNulo(producto, "El producto");
        if (!id.equals(producto.getRestaurantId())) {
            throw new IllegalArgumentException("El producto '" + producto.getNombre() + "' no pertenece al restaurante " + id + ".");
        }
        if (productos.contains(producto)) {
            throw new IllegalArgumentException("El producto con id " + producto.getId() + " ya existe en el menú.");
        }
        if (producto.getCategoria() != null && !categorias.contains(producto.getCategoria())) {
            categorias.add(producto.getCategoria());
        }
        productos.add(producto);
    }

    /**
     * Elimina un producto del menú.
     *
     * @param idProducto Id del producto.
     * @return {@code true} si se eliminó, {@code false} si no existía.
     */
    public boolean eliminarProducto(String idProducto) {
        return productos.removeIf(p -> p.getId().equals(idProducto));
    }

    /**
     * @param idProducto Id del producto.
     * @return El producto del menú, o {@code null} si no existe.
     */
    public Product buscarProducto(String idProducto) {
        for (Product p : productos) {
            if (p.getId().equals(idProducto)) {
                return p;
            }
        }
        return null;
    }

    // ---- Calificaciones (Information Expert) ----

    /** @return Vista de solo lectura de las calificaciones recibidas. */
    public List<Rating> getCalificaciones() {
        return Collections.unmodifiableList(calificaciones);
    }

    /**
     * Registra una calificación y actualiza el promedio del restaurante.
     *
     * @param calificacion Calificación a registrar.
     * @throws IllegalArgumentException Si es nula, corresponde a otro restaurante o ya fue registrada.
     */
    public void agregarCalificacion(Rating calificacion) {
        Validaciones.noNulo(calificacion, "La calificación");
        if (!id.equals(calificacion.getRestaurantId())) {
            throw new IllegalArgumentException("La calificación no corresponde al restaurante " + id + ".");
        }
        if (calificaciones.contains(calificacion)) {
            throw new IllegalArgumentException("La calificación con id " + calificacion.getId() + " ya fue registrada.");
        }
        calificaciones.add(calificacion);
        actualizarPromedio();
    }

    /**
     * Recalcula el promedio de estrellas a partir de las calificaciones registradas.
     *
     * @return El promedio actualizado (0.0 si no hay calificaciones).
     */
    public double actualizarPromedio() {
        if (calificaciones.isEmpty()) {
            promedioCalificacion = 0.0;
        } else {
            int suma = 0;
            for (Rating r : calificaciones) {
                suma += r.getEstrellas();
            }
            promedioCalificacion = (double) suma / calificaciones.size();
        }
        return promedioCalificacion;
    }

    /** @return Promedio de estrellas entre 1.0 y 5.0, o 0.0 si aún no tiene calificaciones. */
    public double getPromedioCalificacion() {
        return promedioCalificacion;
    }

    // ---- Alias de compatibilidad con las capas dao/service existentes ----

    /** @return Alias de {@link #getNombre()}. */
    public String getName() {
        return getNombre();
    }

    /** @param name Alias de {@link #setNombre(String)}. */
    public final void setName(String name) {
        setNombre(name);
    }

    /** @return Alias de {@link #getTipoCocina()}. */
    public String getCategory() {
        return getTipoCocina();
    }

    /** @param category Alias de {@link #setTipoCocina(String)}. */
    public final void setCategory(String category) {
        setTipoCocina(category);
    }

    /** @return Alias de {@link #estaAbierto()}. */
    public boolean isOpen() {
        return estaAbierto();
    }

    /** @param open {@code true} = {@link RestaurantStatus#ABIERTO}, {@code false} = {@link RestaurantStatus#CERRADO}. */
    public final void setOpen(boolean open) {
        setEstado(open ? RestaurantStatus.ABIERTO : RestaurantStatus.CERRADO);
    }

    /**
     * Línea de texto plano usada por {@code RestaurantFileDao}:
     * {@code id;nombre;tipoCocina;abierto}.
     *
     * @return Registro delimitado por punto y coma.
     */
    public String toRegistro() {
        return id + ";" + nombre + ";" + tipoCocina + ";" + estaAbierto();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return id.equals(((Restaurant) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        String calificacion = calificaciones.isEmpty()
                ? "sin calificaciones"
                : String.format("%.1f★ (%d)", promedioCalificacion, calificaciones.size());
        return nombre + " (" + tipoCocina + ") [" + estado + "] - " + calificacion;
    }
}
