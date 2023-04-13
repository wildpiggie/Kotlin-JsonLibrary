enum class UC {
    PA, EPCS, PGMV, ICO, GCCO
}

data class StudentObject(
    val numero: Number,
    val nome: String,
    val internacional: Boolean
)

data class ClassObject(
    val uc: UC,
    val ects: Number,
    @JsonName("data-exame")
    val dataExame: String?,
    val inscritos: MutableCollection<StudentObject> = mutableListOf()
) {
    fun addInscrito(value: StudentObject) = inscritos.add(value)
}

data class CustomClassObject(
    val uc: UC,
    @JsonAsString
    val numberAsString: Number,
    @JsonName("lie")
    val truth: Boolean,
    val student: StudentObject,
    val list: List<Any>,
    @JsonExclude
    val excludedString: String = "excluded"
)
