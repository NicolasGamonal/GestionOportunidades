package duoc.example.gestionoportunidades.util

// Regiones y comunas de Chile (datos entregados por el equipo, 16 regiones / 346 comunas).
//
// ¿Por qué la región NO se guarda en Room?
// La región depende de la comuna (comuna -> región). Guardarla en Organizacion u Oportunidad
// duplicaría información y rompería la 3FN. En cambio, en Room se guarda solo la comuna y la
// región se DERIVA con Regiones.regionDe(comuna). Así hay una única fuente de verdad.

data class RegionChile(
    val id: Int,
    val nombre: String,
    val comunas: List<String>
)

object Regiones {

    val todas: List<RegionChile> = listOf(
        RegionChile(0, "Arica y Parinacota", listOf(
            "Arica", "Camarones", "Putre", "General Lagos"
        )),
        RegionChile(1, "Tarapacá", listOf(
            "Iquique", "Alto Hospicio", "Pozo Almonte", "Camiña", "Colchane", "Huara", "Pica"
        )),
        RegionChile(2, "Antofagasta", listOf(
            "Antofagasta", "Mejillones", "Sierra Gorda", "Taltal", "Calama", "Ollagüe", "San Pedro de Atacama", "Tocopilla", "María Elena"
        )),
        RegionChile(3, "Atacama", listOf(
            "Copiapó", "Caldera", "Tierra Amarilla", "Chañaral", "Diego de Almagro", "Vallenar", "Alto del Carmen", "Freirina", "Huasco"
        )),
        RegionChile(4, "Coquimbo", listOf(
            "La Serena", "Coquimbo", "Andacollo", "La Higuera", "Paiguano", "Vicuña", "Illapel", "Canela", "Los Vilos", "Salamanca", "Ovalle", "Combarbalá", "Monte Patria", "Punitaqui", "Río Hurtado"
        )),
        RegionChile(5, "Valparaíso", listOf(
            "Valparaíso", "Casablanca", "Concón", "Juan Fernández", "Puchuncaví", "Quintero", "Viña del Mar", "Isla de Pascua", "Los Andes", "Calle Larga", "Rinconada", "San Esteban", "La Ligua", "Cabildo", "Papudo", "Petorca", "Zapallar", "Quillota", "Calera", "Hijuelas", "La Cruz", "Nogales", "San Antonio", "Algarrobo", "Cartagena", "El Quisco", "El Tabo", "Santo Domingo", "San Felipe", "Catemu", "Llaillay", "Panquehue", "Putaendo", "Santa María", "Quilpué", "Limache", "Olmué", "Villa Alemana"
        )),
        RegionChile(6, "Región del Libertador Gral. Bernardo O’Higgins", listOf(
            "Rancagua", "Codegua", "Coinco", "Coltauco", "Doñihue", "Graneros", "Las Cabras", "Machalí", "Malloa", "Mostazal", "Olivar", "Peumo", "Pichidegua", "Quinta de Tilcoco", "Rengo", "Requínoa", "San Vicente", "Pichilemu", "La Estrella", "Litueche", "Marchihue", "Navidad", "Paredones", "San Fernando", "Chépica", "Chimbarongo", "Lolol", "Nancagua", "Palmilla", "Peralillo", "Placilla", "Pumanque", "Santa Cruz"
        )),
        RegionChile(7, "Región del Maule", listOf(
            "Talca", "Constitución", "Curepto", "Empedrado", "Maule", "Pelarco", "Pencahue", "Río Claro", "San Clemente", "San Rafael", "Cauquenes", "Chanco", "Pelluhue", "Curicó", "Hualañé", "Licantén", "Molina", "Rauco", "Romeral", "Sagrada Familia", "Teno", "Vichuquén", "Linares", "Colbún", "Longaví", "Parral", "Retiro", "San Javier", "Villa Alegre", "Yerbas Buenas"
        )),
        RegionChile(8, "Región de Ñuble", listOf(
            "Cobquecura", "Coelemu", "Ninhue", "Portezuelo", "Quirihue", "Ránquil", "Treguaco", "Bulnes", "Chillán Viejo", "Chillán", "El Carmen", "Pemuco", "Pinto", "Quillón", "San Ignacio", "Yungay", "Coihueco", "Ñiquén", "San Carlos", "San Fabián", "San Nicolás"
        )),
        RegionChile(9, "Región del Biobío", listOf(
            "Concepción", "Coronel", "Chiguayante", "Florida", "Hualqui", "Lota", "Penco", "San Pedro de la Paz", "Santa Juana", "Talcahuano", "Tomé", "Hualpén", "Lebu", "Arauco", "Cañete", "Contulmo", "Curanilahue", "Los Álamos", "Tirúa", "Los Ángeles", "Antuco", "Cabrero", "Laja", "Mulchén", "Nacimiento", "Negrete", "Quilaco", "Quilleco", "San Rosendo", "Santa Bárbara", "Tucapel", "Yumbel", "Alto Biobío"
        )),
        RegionChile(10, "Región de la Araucanía", listOf(
            "Temuco", "Carahue", "Cunco", "Curarrehue", "Freire", "Galvarino", "Gorbea", "Lautaro", "Loncoche", "Melipeuco", "Nueva Imperial", "Padre las Casas", "Perquenco", "Pitrufquén", "Pucón", "Saavedra", "Teodoro Schmidt", "Toltén", "Vilcún", "Villarrica", "Cholchol", "Angol", "Collipulli", "Curacautín", "Ercilla", "Lonquimay", "Los Sauces", "Lumaco", "Purén", "Renaico", "Traiguén", "Victoria"
        )),
        RegionChile(11, "Región de Los Ríos", listOf(
            "Valdivia", "Corral", "Lanco", "Los Lagos", "Máfil", "Mariquina", "Paillaco", "Panguipulli", "La Unión", "Futrono", "Lago Ranco", "Río Bueno"
        )),
        RegionChile(12, "Región de Los Lagos", listOf(
            "Puerto Montt", "Calbuco", "Cochamó", "Fresia", "Frutillar", "Los Muermos", "Llanquihue", "Maullín", "Puerto Varas", "Castro", "Ancud", "Chonchi", "Curaco de Vélez", "Dalcahue", "Puqueldón", "Queilén", "Quellón", "Quemchi", "Quinchao", "Osorno", "Puerto Octay", "Purranque", "Puyehue", "Río Negro", "San Juan de la Costa", "San Pablo", "Chaitén", "Futaleufú", "Hualaihué", "Palena"
        )),
        RegionChile(13, "Región Aisén del Gral. Carlos Ibáñez del Campo", listOf(
            "Coihaique", "Lago Verde", "Aisén", "Cisnes", "Guaitecas", "Cochrane", "O’Higgins", "Tortel", "Chile Chico", "Río Ibáñez"
        )),
        RegionChile(14, "Región de Magallanes y de la Antártica Chilena", listOf(
            "Punta Arenas", "Laguna Blanca", "Río Verde", "San Gregorio", "Cabo de Hornos (Ex Navarino)", "Antártica", "Porvenir", "Primavera", "Timaukel", "Natales", "Torres del Paine"
        )),
        RegionChile(15, "Región Metropolitana de Santiago", listOf(
            "Cerrillos", "Cerro Navia", "Conchalí", "El Bosque", "Estación Central", "Huechuraba", "Independencia", "La Cisterna", "La Florida", "La Granja", "La Pintana", "La Reina", "Las Condes", "Lo Barnechea", "Lo Espejo", "Lo Prado", "Macul", "Maipú", "Ñuñoa", "Pedro Aguirre Cerda", "Peñalolén", "Providencia", "Pudahuel", "Quilicura", "Quinta Normal", "Recoleta", "Renca", "Santiago", "San Joaquín", "San Miguel", "San Ramón", "Vitacura", "Puente Alto", "Pirque", "San José de Maipo", "Colina", "Lampa", "Tiltil", "San Bernardo", "Buin", "Calera de Tango", "Paine", "Melipilla", "Alhué", "Curacaví", "María Pinto", "San Pedro", "Talagante", "El Monte", "Isla de Maipo", "Padre Hurtado", "Peñaflor"
        ))
    )

    /** Nombres de las 16 regiones, en el orden de la lista original (de norte a sur). */
    val nombres: List<String> = todas.map { it.nombre }

    // Índice inverso comuna -> región. Se construye una sola vez al cargar el objeto.
    private val regionPorComuna: Map<String, String> =
        todas.flatMap { region -> region.comunas.map { comuna -> comuna to region.nombre } }.toMap()

    /** Región a la que pertenece la comuna, o null si la comuna no está en el catálogo. */
    fun regionDe(comuna: String): String? = regionPorComuna[comuna.trim()]

    /** Comunas de una región (lista vacía si la región no existe). */
    fun comunasDe(region: String): List<String> =
        todas.firstOrNull { it.nombre == region }?.comunas ?: emptyList()

    fun esComunaValida(comuna: String): Boolean = regionDe(comuna) != null
}
