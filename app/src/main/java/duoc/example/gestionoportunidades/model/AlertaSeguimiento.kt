package duoc.example.gestionoportunidades.model

data class AlertaSeguimiento(
    val idOportunidad: Long,
    val nombreOportunidad: String,
    val nombreResponsable: String?,
    val tipoAccion: String,
    val proximaAccion: String,
    val fechaProximo: Long
)