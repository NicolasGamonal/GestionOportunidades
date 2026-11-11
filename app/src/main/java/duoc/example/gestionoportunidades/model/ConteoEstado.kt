package duoc.example.gestionoportunidades.model

// Resultado de una consulta con GROUP BY (no es una tabla): cuántas oportunidades hay en cada estado.
// Room llena este objeto haciendo coincidir los alias de la consulta (estado, cantidad) con las
// propiedades. Lo usa el panel de indicadores del supervisor (HU-25).
data class ConteoEstado(
    val estado: String,
    val cantidad: Int
)
