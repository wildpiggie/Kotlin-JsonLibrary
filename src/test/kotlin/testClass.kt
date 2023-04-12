enum class UC {
    PA, EPCS, PGMV, ICO, GCCO
}

data class StudentObject(
    val numero: Number,
    val nome: String,
    val international: Boolean
)

data class ClassObject(
    val uc: UC,
    val ects: Number,
    val dataExame: String?,
    val inscritos: MutableCollection<StudentObject> = mutableListOf()
) {
    fun addInscrito(value: StudentObject) = inscritos.add(value)
}
