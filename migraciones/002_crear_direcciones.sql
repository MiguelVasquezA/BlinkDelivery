CREATE TABLE direccion(
	id_direccion INTEGER PRIMARY KEY,
	id_cliente INTEGER NOT NULL
		REFERENCES cliente (id_cliente) ON DELETE CASCADE,
	alias VARCHAR(40),
	direccion_texto VARCHAR(200) NOT NULL,
	ciudad VARCHAR(60) NOT NULL,
	referencia VARCHAR(150),
	es_principal BOOLEAN NOT NULL DEFAULT FALSE
);