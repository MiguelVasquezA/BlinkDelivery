CREATE TABLE persona (
	id_persona INTEGER PRIMARY KEY,
	nombre VARCHAR(60) NOT NULL,
	apellido VARCHAR(60) NOT NULL,
	telefono VARCHAR(20)
);

CREATE TABLE cliente(
	id_cliente INTEGER PRIMARY KEY
		REFERENCES persona (id_persona) ON DELETE RESTRICT,
	correo VARCHAR(120) NOT NULL
		CONSTRAINT correo_cliente_unico UNIQUE,
	contrasena_hash VARCHAR(255) NOT NULL,
	fecha_registro DATE NOT NULL
);

CREATE TABLE domiciliario(
	id_domiciliario INTEGER PRIMARY KEY
		REFERENCES persona(id_persona) ON DELETE RESTRICT,
	vehiculo VARCHAR(20) NOT NULL
		CONSTRAINT vehiculo_valido
		CHECK(
			vehiculo = 'moto'
			OR vehiculo = 'bicicleta'
			OR vehiculo = 'carro'
		),
	estado VARCHAR(20) NOT NULL DEFAULT 'disponible'
		CONSTRAINT estado_domiciliario_valido
		CHECK(
			estado = 'disponible'
			OR estado = 'en entrega'
			OR estado = 'inactivo'
		),
	fecha_registro DATE NOT NULL
);