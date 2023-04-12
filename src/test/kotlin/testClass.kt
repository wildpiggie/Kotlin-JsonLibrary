enum class UC {
    PA, EPCS, PGMV, ICO, GCCO
}

data class StudentObject(
    val numero: Number,
    val nome: String,
    val international: Boolean
)

data class ClassObject(
    private val uc: UC,
    private val ects: Number,
    private val dataExame: String?,
    private val inscritos: MutableCollection<StudentObject> = mutableListOf()
) {
    fun addInscrito(value: StudentObject) = inscritos.add(value)
}
